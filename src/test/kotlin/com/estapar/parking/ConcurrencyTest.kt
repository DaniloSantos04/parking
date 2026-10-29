package com.estapar.parking

import com.estapar.parking.adapter.out.persistence.repository.AnomalyJpaRepository
import com.estapar.parking.adapter.out.persistence.repository.ParkingSessionJpaRepository
import com.estapar.parking.adapter.out.persistence.repository.SpotJpaRepository
import com.estapar.parking.application.port.`in`.EntryCommand
import com.estapar.parking.application.port.`in`.ExitCommand
import com.estapar.parking.application.port.`in`.ParkedCommand
import com.estapar.parking.application.port.`in`.RegisterEntryUseCase
import com.estapar.parking.application.port.`in`.RegisterExitUseCase
import com.estapar.parking.application.port.`in`.RegisterParkedUseCase
import com.estapar.parking.domain.model.AnomalyType
import com.estapar.parking.domain.model.SessionStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@IntegrationTest
class ConcurrencyTest(
    @Autowired private val registerEntry: RegisterEntryUseCase,
    @Autowired private val registerParked: RegisterParkedUseCase,
    @Autowired private val registerExit: RegisterExitUseCase,
    @Autowired private val sessionRepository: ParkingSessionJpaRepository,
    @Autowired private val anomalyRepository: AnomalyJpaRepository,
    @Autowired private val spotRepository: SpotJpaRepository,
    @Autowired private val databaseCleaner: DatabaseCleaner,
) {
    private val entryTime = Instant.parse("2025-01-01T12:00:00Z")
    private val spot1 = GarageFixtures.SPOTS.first().coordinates

    @BeforeEach
    fun cleanDatabase() = databaseCleaner.clean()

    private fun runConcurrently(tasks: List<() -> Unit>) {
        val executor = Executors.newFixedThreadPool(tasks.size)
        val start = CountDownLatch(1)
        try {
            val futures = tasks.map { task -> executor.submit { start.await(); task() } }
            start.countDown()
            futures.forEach { it.get(60, TimeUnit.SECONDS) }
        } finally {
            executor.shutdownNow()
        }
    }

    private fun anomalyCount(type: AnomalyType) = anomalyRepository.findAll().count { it.type == type }

    private fun activeSessions() = sessionRepository.findAll().filter { it.status != SessionStatus.EXITED }

    @Test
    fun `20 entradas simultaneas com 5 vagas aceitam exatamente 5`() {
        runConcurrently((1..20).map { i -> { registerEntry.registerEntry(EntryCommand("CAR%04d".format(i), entryTime)) } })

        assertEquals(5, sessionRepository.count())
        assertEquals(15, anomalyCount(AnomalyType.GARAGE_FULL))
        assertEquals(
            listOf("0.90", "0.90", "1.00", "1.10", "1.25").map(::BigDecimal),
            sessionRepository.findAll().map { it.priceMultiplier }.sorted(),
        )
    }

    @Test
    fun `ENTRY simultaneo da mesma placa cria uma unica sessao`() {
        runConcurrently(List(10) { { registerEntry.registerEntry(EntryCommand("ZUL0001", entryTime)) } })

        assertEquals(1, sessionRepository.count())
        assertEquals(9, anomalyCount(AnomalyType.DUPLICATE_ENTRY))
    }

    @Test
    fun `PARKED simultaneo na mesma vaga ocupa a vaga uma vez so`() {
        val plates = (1..5).map { "CAR%04d".format(it) }
        plates.forEach { registerEntry.registerEntry(EntryCommand(it, entryTime)) }

        runConcurrently(plates.map { plate -> { registerParked.registerParked(ParkedCommand(plate, spot1)) } })

        assertEquals(1, sessionRepository.findAll().count { it.status == SessionStatus.PARKED && it.spotId == 1L })
        assertEquals(4, anomalyCount(AnomalyType.SPOT_OCCUPIED))
        assertTrue(spotRepository.findById(1).orElseThrow().occupied)
    }

    @Test
    fun `saidas e entradas simultaneas com a garagem cheia nunca passam da capacidade`() {
        val inside = (1..5).map { "IN%04d".format(it) }
        inside.forEachIndexed { i, plate ->
            registerEntry.registerEntry(EntryCommand(plate, entryTime))
            registerParked.registerParked(ParkedCommand(plate, GarageFixtures.SPOTS[i].coordinates))
        }
        val exitTime = entryTime.plusSeconds(3600)
        val newcomers = (1..10).map { "NEW%04d".format(it) }

        runConcurrently(
            inside.map { plate -> { registerExit.registerExit(ExitCommand(plate, exitTime)) } } +
                newcomers.map { plate -> { registerEntry.registerEntry(EntryCommand(plate, exitTime)) } },
        )

        val active = activeSessions()
        val accepted = active.count { it.licensePlate.startsWith("NEW") }
        assertTrue(active.size <= 5, "lotação final ${active.size} passou da capacidade 5")
        assertEquals(10, accepted + anomalyCount(AnomalyType.GARAGE_FULL))
        assertEquals(5, sessionRepository.findAll().count { it.status == SessionStatus.EXITED })
        assertEquals(0, spotRepository.findAll().count { it.occupied })
    }
}
