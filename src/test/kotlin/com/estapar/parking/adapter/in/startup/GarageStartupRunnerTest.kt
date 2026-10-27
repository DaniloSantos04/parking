package com.estapar.parking.adapter.`in`.startup

import com.estapar.parking.GarageFixtures
import com.estapar.parking.application.port.`in`.LoadGarageUseCase
import com.estapar.parking.config.SimulatorProperties
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.boot.DefaultApplicationArguments
import java.time.Duration
import kotlin.test.assertContains
import kotlin.test.assertEquals

class GarageStartupRunnerTest {

    private val loadGarage = mockk<LoadGarageUseCase>()
    private val properties = SimulatorProperties(
        baseUrl = "http://simulador:3000",
        retry = SimulatorProperties.Retry(maxAttempts = 5, interval = Duration.ZERO),
    )
    private val runner = GarageStartupRunner(loadGarage, properties)
    private val args = DefaultApplicationArguments()

    @Test
    fun `carrega na primeira tentativa`() {
        every { loadGarage.load() } returns GarageFixtures.CONFIGURATION

        runner.run(args)

        verify(exactly = 1) { loadGarage.load() }
    }

    @Test
    fun `tenta de novo ate conseguir`() {
        every { loadGarage.load() } throws RuntimeException("connection refused") andThenThrows
            RuntimeException("connection refused") andThen GarageFixtures.CONFIGURATION

        runner.run(args)

        verify(exactly = 3) { loadGarage.load() }
    }

    @Test
    fun `falha a subida depois de esgotar as tentativas, informando url e motivo`() {
        every { loadGarage.load() } throws RuntimeException("connection refused")

        val error = assertThrows<GarageLoadException> { runner.run(args) }

        verify(exactly = 5) { loadGarage.load() }
        assertContains(error.message!!, "http://simulador:3000")
        assertContains(error.message!!, "5 tentativas")
        assertContains(error.message!!, "connection refused")
        assertEquals("connection refused", error.cause?.message)
    }

    @Test
    fun `informa a excecao e a causa raiz`() {
        every { loadGarage.load() } throws RuntimeException("I/O error", java.net.ConnectException(null))

        val error = assertThrows<GarageLoadException> { runner.run(args) }

        assertContains(error.message!!, "RuntimeException: I/O error (causa raiz: ConnectException: sem mensagem)")
    }

    @Test
    fun `espera o intervalo configurado entre as tentativas`() {
        val interval = Duration.ofMillis(200)
        val slowRunner = GarageStartupRunner(
            loadGarage,
            properties.copy(retry = SimulatorProperties.Retry(maxAttempts = 3, interval = interval)),
        )
        every { loadGarage.load() } throws RuntimeException("connection refused")

        val start = System.nanoTime()
        assertThrows<GarageLoadException> { slowRunner.run(args) }
        val elapsed = Duration.ofNanos(System.nanoTime() - start)

        // 3 tentativas, 2 esperas entre elas, nenhuma depois da última.
        assert(elapsed >= interval.multipliedBy(2)) { "esperou só $elapsed" }
        assert(elapsed < interval.multipliedBy(3)) { "esperou demais: $elapsed" }
    }
}
