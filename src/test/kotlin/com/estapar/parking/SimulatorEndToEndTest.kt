package com.estapar.parking

import com.estapar.parking.adapter.out.persistence.entity.GarageEntity
import com.estapar.parking.adapter.out.persistence.repository.AnomalyJpaRepository
import com.estapar.parking.adapter.out.persistence.repository.GarageJpaRepository
import com.estapar.parking.adapter.out.persistence.repository.ParkingSessionJpaRepository
import com.estapar.parking.adapter.out.persistence.repository.SectorJpaRepository
import com.estapar.parking.adapter.out.persistence.repository.SpotJpaRepository
import com.estapar.parking.domain.model.SessionStatus
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import org.testcontainers.Testcontainers
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import java.math.BigDecimal
import java.net.ServerSocket
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Ponta a ponta com o simulador real (`cfontes0estapar/garage-sim:1.0.0`) num container: a
 * aplicação inteira sobe, carrega o GET /garage do simulador e recebe os eventos reais no webhook,
 * gravando no MySQL real. Leva cerca de 1 minuto, porque espera o simulador gerar saídas.
 *
 * O simulador só começa a enviar eventos depois do primeiro GET /garage, que é o da aplicação ao
 * subir. Por isso o container espera pelo log de inicialização, e não por uma chamada HTTP.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Import(TestcontainersConfiguration::class)
@ExtendWith(OutputCaptureExtension::class)
class SimulatorEndToEndTest(
    @Autowired private val garageRepository: GarageJpaRepository,
    @Autowired private val sectorRepository: SectorJpaRepository,
    @Autowired private val spotRepository: SpotJpaRepository,
    @Autowired private val sessionRepository: ParkingSessionJpaRepository,
    @Autowired private val anomalyRepository: AnomalyJpaRepository,
) {

    companion object {
        private const val SIMULATOR_PORT = 3000
        private val appPort = ServerSocket(0).use { it.localPort }

        private val simulator: GenericContainer<*> = GenericContainer("cfontes0estapar/garage-sim:1.0.0")
            .withExposedPorts(SIMULATOR_PORT)
            .withEnv("CLIENT_WEBHOOK_URL", "http://host.testcontainers.internal:$appPort/webhook")
            .waitingFor(Wait.forLogMessage(".*Simulator starting.*", 1))

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            // O container precisa enxergar a porta da aplicação no host antes de subir.
            Testcontainers.exposeHostPorts(appPort)
            simulator.start()
            registry.add("server.port") { appPort }
            registry.add("estapar.simulator.base-url") { "http://${simulator.host}:${simulator.getMappedPort(SIMULATOR_PORT)}" }
        }
    }

    private val api = RestClient.create("http://localhost:$appPort")

    @Test
    fun `processa os eventos reais do simulador de ponta a ponta`(output: CapturedOutput) {
        // Carga inicial com os dados reais do simulador.
        assertEquals(setOf("A", "B"), sectorRepository.findAll().map { it.code }.toSet())
        assertEquals(30, spotRepository.count())
        assertEquals(30, garageRepository.findById(GarageEntity.SINGLETON_ID).orElseThrow().capacity)

        waitUntil(Duration.ofSeconds(90)) {
            sessionRepository.findAll().count { it.status == SessionStatus.EXITED } >= 3
        }

        val sessions = sessionRepository.findAll()
        val parked = sessions.filter { it.status == SessionStatus.PARKED }
        val exited = sessions.filter { it.status == SessionStatus.EXITED }

        // Todos os eventos foram aceitos e aplicados, sem nenhuma anomalia.
        assertFalse(output.out.contains("Requisição rejeitada"), "algum evento do simulador recebeu 400")
        assertEquals(emptyList(), anomalyRepository.findAll().map { it.type })

        // Ocupação das vagas coerente com as sessões estacionadas.
        assertEquals(parked.size, spotRepository.findAll().count { it.occupied })
        assertEquals(parked.size, parked.mapNotNull { it.spotId }.toSet().size)

        // Saídas cobradas: setor e preço do PARKED, valor calculado, saída depois da entrada.
        exited.forEach {
            assertTrue(it.sectorCode in setOf("A", "B"))
            assertTrue(it.amount != null && it.amount!!.scale() == 2)
            assertFalse(it.exitTime!!.isBefore(it.entryTime))
        }

        // A receita de hoje bate com a soma das saídas gravadas.
        val revenue = api.get().uri("/revenue").retrieve().body<Map<String, Any>>()!!
        assertEquals("BRL", revenue["currency"])
        assertEquals(
            exited.fold(BigDecimal.ZERO) { sum, s -> sum + s.amount!! }.setScale(2),
            BigDecimal(revenue["amount"].toString()).setScale(2),
        )
    }

    private fun waitUntil(timeout: Duration, condition: () -> Boolean) {
        val deadline = System.nanoTime() + timeout.toNanos()
        while (!condition()) {
            check(System.nanoTime() < deadline) { "Condição não atingida em $timeout" }
            Thread.sleep(1_000)
        }
    }
}
