package com.estapar.parking.adapter.`in`.web

import com.estapar.parking.IntegrationTest
import com.estapar.parking.adapter.out.persistence.entity.ParkingSessionEntity
import com.estapar.parking.adapter.out.persistence.repository.AnomalyJpaRepository
import com.estapar.parking.adapter.out.persistence.repository.ParkingSessionJpaRepository
import com.estapar.parking.domain.model.AnomalyType
import com.estapar.parking.domain.model.SessionStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.post
import java.math.BigDecimal
import java.time.Instant
import kotlin.test.assertContains
import kotlin.test.assertEquals

/** ENTRY ponta a ponta: HTTP → serviço → MySQL. A garagem dos testes tem capacidade 5. */
@IntegrationTest
class WebhookEntryTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val sessionRepository: ParkingSessionJpaRepository,
    @Autowired private val anomalyRepository: AnomalyJpaRepository,
) {

    @BeforeEach
    fun cleanDatabase() {
        sessionRepository.deleteAllInBatch()
        anomalyRepository.deleteAllInBatch()
    }

    private fun post(body: String): ResultActionsDsl = mockMvc.post("/webhook") {
        contentType = MediaType.APPLICATION_JSON
        content = body
    }

    private fun entry(plate: String, time: String = "2025-01-01T12:00:00.000Z") =
        post("""{"license_plate": "$plate", "entry_time": "$time", "event_type": "ENTRY"}""")

    @Test
    fun `registra a entrada no formato do enunciado`() {
        entry("ZUL0001").andExpect { status { isOk() } }

        val session = sessionRepository.findAll().single()
        assertEquals("ZUL0001", session.licensePlate)
        assertEquals(SessionStatus.ENTERED, session.status)
        assertEquals(Instant.parse("2025-01-01T12:00:00Z"), session.entryTime)
        assertEquals(BigDecimal("0.90"), session.priceMultiplier)
    }

    @Test
    fun `registra a entrada no formato real do simulador, com a data em UTC`() {
        post("""{"license_plate":"SKX77845","entry_time":"2026-10-27T22:00:24","event_type":"ENTRY"}""")
            .andExpect { status { isOk() } }

        assertEquals(Instant.parse("2026-10-27T22:00:24Z"), sessionRepository.findAll().single().entryTime)
    }

    @Test
    fun `normaliza a placa`() {
        entry(" zul0001 ").andExpect { status { isOk() } }

        assertEquals("ZUL0001", sessionRepository.findAll().single().licensePlate)
    }

    @Test
    fun `trava o multiplicador pela lotacao antes de cada entrada`() {
        (1..5).forEach { entry("CAR000$it").andExpect { status { isOk() } } }

        // Capacidade 5: antes de cada entrada havia 0, 1, 2, 3 e 4 carros (0%, 20%, 40%, 60% e 80%).
        val multipliers = sessionRepository.findAll().sortedBy { it.id }.map { it.priceMultiplier }
        assertEquals(listOf("0.90", "0.90", "1.00", "1.10", "1.25").map(::BigDecimal), multipliers)
    }

    @Test
    fun `com a garagem cheia responde 200, nao cria sessao e registra anomalia`() {
        (1..5).forEach { entry("CAR000$it") }

        entry("CAR0006").andExpect { status { isOk() } }

        assertEquals(5, sessionRepository.count())
        val anomaly = anomalyRepository.findAll().single()
        assertEquals(AnomalyType.GARAGE_FULL, anomaly.type)
        assertEquals("CAR0006", anomaly.licensePlate)
    }

    @Test
    fun `ENTRY repetido da mesma placa responde 200, mantem uma sessao e registra anomalia`() {
        entry("ZUL0001")

        entry("ZUL0001", time = "2025-01-01T12:05:00.000Z").andExpect { status { isOk() } }

        assertEquals(1, sessionRepository.count())
        assertEquals(Instant.parse("2025-01-01T12:00:00Z"), sessionRepository.findAll().single().entryTime)
        assertEquals(AnomalyType.DUPLICATE_ENTRY, anomalyRepository.findAll().single().type)
    }

    @Test
    fun `o banco garante no maximo uma sessao ativa por placa`() {
        fun activeSession() = ParkingSessionEntity(
            licensePlate = "ZUL0001",
            status = SessionStatus.ENTERED,
            entryTime = Instant.parse("2025-01-01T12:00:00Z"),
            priceMultiplier = BigDecimal("1.00"),
        )
        sessionRepository.saveAndFlush(activeSession())

        assertThrows<DataIntegrityViolationException> { sessionRepository.saveAndFlush(activeSession()) }
    }

    @Test
    @ExtendWith(OutputCaptureExtension::class)
    fun `loga em INFO cada evento recebido`(output: CapturedOutput) {
        entry("ZUL0001")

        assertContains(output.out, "INFO")
        assertContains(output.out, "Evento recebido no webhook: EntryEvent(licensePlate=ZUL0001, entryTime=2025-01-01T12:00:00Z)")
    }

    @Test
    @ExtendWith(OutputCaptureExtension::class)
    fun `loga em WARN o payload rejeitado com 400`(output: CapturedOutput) {
        post("""{"license_plate": "ZUL0001", "event_type": "ENTRY"}""")

        assertContains(output.out, "Requisição rejeitada com 400 em uri=/webhook: Campo ausente ou inválido: entry_time")
    }

    @Test
    fun `JSON malformado responde 400 com problem details`() {
        post("""{"license_plate": "ZUL0001", """).andExpect {
            status { isBadRequest() }
            content { contentType(MediaType.APPLICATION_PROBLEM_JSON) }
        }
    }

    @Test
    fun `event_type desconhecido responde 400`() {
        post("""{"license_plate": "ZUL0001", "event_type": "TELEPORT"}""").andExpect {
            status { isBadRequest() }
            jsonPath("$.detail") { value("event_type ausente ou desconhecido; esperado ENTRY, PARKED ou EXIT") }
        }
    }

    @Test
    fun `ENTRY sem placa responde 400 apontando o campo`() {
        post("""{"entry_time": "2025-01-01T12:00:00.000Z", "event_type": "ENTRY"}""").andExpect {
            status { isBadRequest() }
            jsonPath("$.detail") { value("Campo ausente ou inválido: license_plate") }
        }
    }

    @Test
    fun `ENTRY com placa em branco responde 400`() {
        entry("   ").andExpect {
            status { isBadRequest() }
            jsonPath("$.detail") { value(org.hamcrest.Matchers.startsWith("licensePlate:")) }
        }
        assertEquals(0, sessionRepository.count())
    }

    @Test
    fun `ENTRY com placa maior que 20 caracteres responde 400`() {
        entry("A".repeat(21)).andExpect { status { isBadRequest() } }
    }

    @Test
    fun `ENTRY sem horario responde 400 apontando o campo`() {
        post("""{"license_plate": "ZUL0001", "event_type": "ENTRY"}""").andExpect {
            status { isBadRequest() }
            jsonPath("$.detail") { value("Campo ausente ou inválido: entry_time") }
        }
    }

    @Test
    fun `ENTRY com horario invalido responde 400 apontando o campo`() {
        entry("ZUL0001", time = "ontem").andExpect {
            status { isBadRequest() }
            jsonPath("$.detail") { value("Campo ausente ou inválido: entry_time") }
        }
    }
}
