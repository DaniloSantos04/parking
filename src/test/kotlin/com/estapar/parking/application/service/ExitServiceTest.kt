package com.estapar.parking.application.service

import com.estapar.parking.GarageFixtures
import com.estapar.parking.application.port.`in`.ExitCommand
import com.estapar.parking.application.port.out.LockGaragePort
import com.estapar.parking.application.port.out.SectorPort
import com.estapar.parking.application.port.out.SessionPort
import com.estapar.parking.application.port.out.SpotPort
import com.estapar.parking.domain.model.AnomalyType
import com.estapar.parking.domain.model.ParkingSession
import com.estapar.parking.domain.model.SessionStatus
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
import kotlin.test.assertNull

class ExitServiceTest {

    private val lockGarage = mockk<LockGaragePort>()
    private val sessions = mockk<SessionPort>()
    private val spots = mockk<SpotPort>()
    private val sectors = mockk<SectorPort>()
    private val anomalies = mockk<AnomalyRecorder>()
    private val service = ExitService(lockGarage, sessions, spots, sectors, anomalies)

    private val entryTime = Instant.parse("2025-01-01T12:00:00Z")
    private val spot = GarageFixtures.SPOTS.first()
    private val entered = ParkingSession.enter("ZUL0001", entryTime, BigDecimal("0.90"))
    private val parked = entered.park(spot, BigDecimal("10.00"))
    private val saved = slot<ParkingSession>()

    @BeforeEach
    fun setUp() {
        every { lockGarage.lockAndGetCapacity() } returns 5
        every { sessions.findActive("ZUL0001") } returns parked
        every { sessions.save(capture(saved)) } answers { firstArg() }
        every { sectors.findLowestBasePrice() } returns BigDecimal("4.10")
        justRun { spots.release(any()) }
        justRun { anomalies.record(any(), any(), any()) }
    }

    private fun exitAfter(seconds: Long) = service.registerExit(ExitCommand("ZUL0001", entryTime.plusSeconds(seconds)))

    @Test
    fun `encerra a sessao com o valor calculado e libera a vaga`() {
        exitAfter(90 * 60)

        assertEquals(SessionStatus.EXITED, saved.captured.status)
        assertEquals(entryTime.plusSeconds(90 * 60), saved.captured.exitTime)
        assertEquals(BigDecimal("18.00"), saved.captured.amount)
        verify { spots.release(spot.id) }
        verify(exactly = 0) { anomalies.record(any(), any(), any()) }
    }

    @Test
    fun `bloqueia a garagem antes de tudo`() {
        exitAfter(60)

        verifyOrder {
            lockGarage.lockAndGetCapacity()
            sessions.findActive("ZUL0001")
            sessions.save(any())
            spots.release(spot.id)
        }
    }

    @Test
    fun `EXIT sem sessao ativa vira anomalia e nada muda`() {
        every { sessions.findActive("ZUL0001") } returns null

        exitAfter(60)

        verify(exactly = 0) { sessions.save(any()) }
        verify(exactly = 0) { spots.release(any()) }
        verify { anomalies.record(AnomalyType.EXIT_WITHOUT_ACTIVE_SESSION, "ZUL0001", any()) }
    }

    @Test
    fun `EXIT sem PARKED cobra pelo menor preco-base, sem setor, e registra anomalia`() {
        every { sessions.findActive("ZUL0001") } returns entered

        exitAfter(3600)

        assertEquals(SessionStatus.EXITED, saved.captured.status)
        assertNull(saved.captured.sectorCode)
        assertEquals(BigDecimal("4.10"), saved.captured.basePrice)
        assertEquals(BigDecimal("3.69"), saved.captured.amount)
        verify(exactly = 0) { spots.release(any()) }
        verify { anomalies.record(AnomalyType.EXIT_WITHOUT_PARKED, "ZUL0001", match { "3.69" in it }) }
    }

    @Test
    fun `EXIT anterior ao ENTRY encerra sem cobranca, libera a vaga e registra anomalia`() {
        exitAfter(-60)

        assertEquals(SessionStatus.EXITED, saved.captured.status)
        assertEquals(BigDecimal("0.00"), saved.captured.amount)
        verify { spots.release(spot.id) }
        verify { anomalies.record(AnomalyType.EXIT_BEFORE_ENTRY, "ZUL0001", any()) }
    }
}
