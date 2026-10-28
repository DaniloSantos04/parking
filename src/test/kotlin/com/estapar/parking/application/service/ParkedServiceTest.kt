package com.estapar.parking.application.service

import com.estapar.parking.GarageFixtures
import com.estapar.parking.application.port.`in`.ParkedCommand
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

class ParkedServiceTest {

    private val lockGarage = mockk<LockGaragePort>()
    private val sessions = mockk<SessionPort>()
    private val spots = mockk<SpotPort>()
    private val sectors = mockk<SectorPort>()
    private val anomalies = mockk<AnomalyRecorder>()
    private val service = ParkedService(lockGarage, sessions, spots, sectors, anomalies)

    private val spot = GarageFixtures.SPOTS.first()
    private val command = ParkedCommand("ZUL0001", spot.coordinates)
    private val entered = ParkingSession.enter("ZUL0001", Instant.parse("2025-01-01T12:00:00Z"), BigDecimal("0.90"))

    @BeforeEach
    fun setUp() {
        every { lockGarage.lockAndGetCapacity() } returns 5
        every { sessions.findActive("ZUL0001") } returns entered
        every { sessions.save(any()) } answers { firstArg() }
        every { spots.findByCoordinates(spot.coordinates) } returns spot
        every { spots.isOccupied(spot.id) } returns false
        justRun { spots.markOccupied(any()) }
        every { sectors.findByCode("A") } returns GarageFixtures.SECTOR_A
        justRun { anomalies.record(any(), any(), any()) }
    }

    @Test
    fun `estaciona na vaga das coordenadas com o preco-base do setor e ocupa a vaga`() {
        val saved = slot<ParkingSession>()
        every { sessions.save(capture(saved)) } answers { firstArg() }

        service.registerParked(command)

        assertEquals(SessionStatus.PARKED, saved.captured.status)
        assertEquals(spot.id, saved.captured.spotId)
        assertEquals("A", saved.captured.sectorCode)
        assertEquals(BigDecimal("10.00"), saved.captured.basePrice)
        verify { spots.markOccupied(spot.id) }
        verify(exactly = 0) { anomalies.record(any(), any(), any()) }
    }

    @Test
    fun `bloqueia a garagem antes de tudo`() {
        service.registerParked(command)

        verifyOrder {
            lockGarage.lockAndGetCapacity()
            sessions.findActive("ZUL0001")
            spots.findByCoordinates(spot.coordinates)
            sessions.save(any())
        }
    }

    @Test
    fun `PARKED sem sessao ativa vira anomalia`() {
        every { sessions.findActive("ZUL0001") } returns null

        service.registerParked(command)

        assertNothingChanged()
        verify { anomalies.record(AnomalyType.PARKED_WITHOUT_ENTRY, "ZUL0001", any()) }
    }

    @Test
    fun `PARKED de carro ja estacionado vira anomalia e mantem a vaga original`() {
        every { sessions.findActive("ZUL0001") } returns entered.park(GarageFixtures.SPOTS[1], BigDecimal("10.00"))

        service.registerParked(command)

        assertNothingChanged()
        verify { anomalies.record(AnomalyType.ALREADY_PARKED, "ZUL0001", match { "vaga 2" in it }) }
    }

    @Test
    fun `PARKED em coordenadas sem vaga vira anomalia`() {
        every { spots.findByCoordinates(any()) } returns null

        service.registerParked(command)

        assertNothingChanged()
        verify { anomalies.record(AnomalyType.SPOT_NOT_FOUND, "ZUL0001", any()) }
    }

    @Test
    fun `PARKED em vaga ocupada vira anomalia e a sessao continua ENTERED`() {
        every { spots.isOccupied(spot.id) } returns true

        service.registerParked(command)

        assertNothingChanged()
        verify { anomalies.record(AnomalyType.SPOT_OCCUPIED, "ZUL0001", match { "vaga 1" in it }) }
    }

    private fun assertNothingChanged() {
        verify(exactly = 0) { sessions.save(any()) }
        verify(exactly = 0) { spots.markOccupied(any()) }
    }
}
