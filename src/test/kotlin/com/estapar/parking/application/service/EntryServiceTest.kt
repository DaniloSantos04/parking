package com.estapar.parking.application.service

import com.estapar.parking.application.port.`in`.EntryCommand
import com.estapar.parking.application.port.out.LockGaragePort
import com.estapar.parking.application.port.out.SessionPort
import com.estapar.parking.domain.model.AnomalyType
import com.estapar.parking.domain.model.ParkingSession
import com.estapar.parking.domain.model.SessionStatus
import com.estapar.parking.domain.pricing.OccupancyPricingPolicy
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import kotlin.test.assertEquals

class EntryServiceTest {

    private val lockGarage = mockk<LockGaragePort>()
    private val sessions = mockk<SessionPort>()
    private val anomalies = mockk<AnomalyRecorder>()
    private val service = EntryService(lockGarage, sessions, OccupancyPricingPolicy(), anomalies)

    private val entryTime = Instant.parse("2025-01-01T12:00:00Z")
    private val command = EntryCommand("ZUL0001", entryTime)

    @BeforeEach
    fun setUp() {
        every { lockGarage.lockAndGetCapacity() } returns 100
        every { sessions.findActive(any()) } returns null
        every { sessions.save(any()) } answers { firstArg() }
        justRun { anomalies.record(any(), any(), any()) }
    }

    @Test
    fun `cria sessao ENTERED com o multiplicador da lotacao antes da entrada`() {
        every { sessions.countActive() } returns 24
        val saved = slot<ParkingSession>()
        every { sessions.save(capture(saved)) } answers { firstArg() }

        service.registerEntry(command)

        assertEquals("ZUL0001", saved.captured.licensePlate)
        assertEquals(SessionStatus.ENTERED, saved.captured.status)
        assertEquals(entryTime, saved.captured.entryTime)
        assertEquals(BigDecimal("0.90"), saved.captured.priceMultiplier)
        verify(exactly = 0) { anomalies.record(any(), any(), any()) }
    }

    @Test
    fun `bloqueia a garagem antes de consultar e gravar`() {
        every { sessions.countActive() } returns 0

        service.registerEntry(command)

        verifyOrder {
            lockGarage.lockAndGetCapacity()
            sessions.findActive("ZUL0001")
            sessions.countActive()
            sessions.save(any())
        }
    }

    @Test
    fun `recusa a entrada com a garagem cheia e registra anomalia`() {
        every { sessions.countActive() } returns 100

        service.registerEntry(command)

        verify(exactly = 0) { sessions.save(any()) }
        verify { anomalies.record(AnomalyType.GARAGE_FULL, "ZUL0001", match { "100/100" in it }) }
    }

    @Test
    fun `ignora ENTRY de placa com sessao ativa e registra anomalia`() {
        every { sessions.findActive("ZUL0001") } returns ParkingSession.enter("ZUL0001", entryTime, BigDecimal("1.00"))

        service.registerEntry(command)

        verify(exactly = 0) { sessions.save(any()) }
        verify(exactly = 0) { sessions.countActive() }
        verify { anomalies.record(AnomalyType.DUPLICATE_ENTRY, "ZUL0001", any()) }
    }
}
