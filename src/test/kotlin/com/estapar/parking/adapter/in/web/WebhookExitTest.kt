package com.estapar.parking.adapter.`in`.web

import com.estapar.parking.DatabaseCleaner
import com.estapar.parking.IntegrationTest
import com.estapar.parking.WebhookClient
import com.estapar.parking.adapter.out.persistence.repository.AnomalyJpaRepository
import com.estapar.parking.adapter.out.persistence.repository.ParkingSessionJpaRepository
import com.estapar.parking.adapter.out.persistence.repository.SpotJpaRepository
import com.estapar.parking.domain.model.AnomalyType
import com.estapar.parking.domain.model.SessionStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.MockMvc
import java.math.BigDecimal
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

/**
 * EXIT ponta a ponta: HTTP → serviço → MySQL. Garagem dos testes: capacidade 5; vagas 1 e 2 no
 * setor A (R$ 10,00), 3 a 5 no setor B (R$ 4,10).
 */
@IntegrationTest
class WebhookExitTest(
    @Autowired mockMvc: MockMvc,
    @Autowired private val sessionRepository: ParkingSessionJpaRepository,
    @Autowired private val anomalyRepository: AnomalyJpaRepository,
    @Autowired private val spotRepository: SpotJpaRepository,
    @Autowired private val databaseCleaner: DatabaseCleaner,
) {
    private val webhook = WebhookClient(mockMvc)

    @BeforeEach
    fun cleanDatabase() = databaseCleaner.clean()

    private fun sessions(plate: String) = sessionRepository.findAll().filter { it.licensePlate == plate }.sortedBy { it.id }

    private fun anomalyTypes() = anomalyRepository.findAll().map { it.type }

    @Test
    fun `fluxo completo ENTRY, PARKED e EXIT cobra, encerra e libera a vaga`() {
        webhook.entry("ZUL0001", "2025-01-01T12:00:00.000Z")
        webhook.parked("ZUL0001", "-23.561644", "-46.655941")

        webhook.exit("ZUL0001", "2025-01-01T14:30:00.000Z").andExpect { status { isOk() } }

        val session = sessions("ZUL0001").single()
        assertEquals(SessionStatus.EXITED, session.status)
        assertEquals(Instant.parse("2025-01-01T14:30:00Z"), session.exitTime)
        assertEquals("B", session.sectorCode)
        // Garagem vazia na entrada (0,90); 2h30 = 3 horas cobradas × R$ 4,10 × 0,90 = 11,07.
        assertEquals(BigDecimal("11.07"), session.amount)
        assertFalse(spotRepository.findById(3).orElseThrow().occupied)
        assertEquals(emptyList(), anomalyTypes())
    }

    @Test
    fun `permanencia de ate 30 minutos sai de graca`() {
        webhook.entry("ZUL0001", "2025-01-01T12:00:00.000Z")
        webhook.parked("ZUL0001", "-23.561684", "-46.655981")

        webhook.exit("ZUL0001", "2025-01-01T12:30:00.000Z").andExpect { status { isOk() } }

        assertEquals(BigDecimal("0.00"), sessions("ZUL0001").single().amount)
    }

    @Test
    fun `EXIT no formato real do simulador, com a data em UTC`() {
        webhook.entry("SKX77845", "2026-10-27T22:00:24")
        webhook.parked("SKX77845", "-23.561684", "-46.655981")

        webhook.post("""{"license_plate":"SKX77845","exit_time":"2026-10-27T23:00:25","event_type":"EXIT"}""")
            .andExpect { status { isOk() } }

        val session = sessions("SKX77845").single()
        assertEquals(Instant.parse("2026-10-27T23:00:25Z"), session.exitTime)
        // 1h e 1s = 2 horas × R$ 10,00 × 0,90.
        assertEquals(BigDecimal("18.00"), session.amount)
    }

    @Test
    fun `garagem cheia reabre depois de uma saida`() {
        (1..5).forEach { webhook.entry("CAR000$it") }
        webhook.entry("CAR0006")
        assertEquals(listOf(AnomalyType.GARAGE_FULL), anomalyTypes())

        webhook.exit("CAR0001", "2025-01-01T13:00:00.000Z")
        webhook.entry("CAR0006").andExpect { status { isOk() } }

        val reentered = sessions("CAR0006").single()
        assertEquals(SessionStatus.ENTERED, reentered.status)
        // Lotação antes da entrada: 4 de 5 (80%) → +25%.
        assertEquals(BigDecimal("1.25"), reentered.priceMultiplier)
    }

    @Test
    fun `a mesma placa pode entrar de novo depois de sair`() {
        webhook.entry("ZUL0001", "2025-01-01T12:00:00.000Z")
        webhook.exit("ZUL0001", "2025-01-01T12:10:00.000Z")

        webhook.entry("ZUL0001", "2025-01-01T15:00:00.000Z").andExpect { status { isOk() } }

        assertEquals(listOf(SessionStatus.EXITED, SessionStatus.ENTERED), sessions("ZUL0001").map { it.status })
    }

    @Test
    fun `EXIT sem PARKED cobra pelo menor preco-base, fica sem setor e registra anomalia`() {
        webhook.entry("ZUL0001", "2025-01-01T12:00:00.000Z")

        webhook.exit("ZUL0001", "2025-01-01T13:00:00.000Z").andExpect { status { isOk() } }

        val session = sessions("ZUL0001").single()
        assertEquals(SessionStatus.EXITED, session.status)
        assertNull(session.sectorCode)
        assertEquals(BigDecimal("4.10"), session.basePrice)
        // 1 hora × menor preço-base (R$ 4,10) × 0,90.
        assertEquals(BigDecimal("3.69"), session.amount)
        assertEquals(listOf(AnomalyType.EXIT_WITHOUT_PARKED), anomalyTypes())
    }

    @Test
    fun `EXIT sem sessao ativa responde 200 e registra anomalia`() {
        webhook.exit("ZUL0001", "2025-01-01T13:00:00.000Z").andExpect { status { isOk() } }

        assertEquals(0, sessionRepository.count())
        assertEquals(listOf(AnomalyType.EXIT_WITHOUT_ACTIVE_SESSION), anomalyTypes())
    }

    @Test
    fun `EXIT repetido nao cobra de novo e registra anomalia`() {
        webhook.entry("ZUL0001", "2025-01-01T12:00:00.000Z")
        webhook.parked("ZUL0001", "-23.561684", "-46.655981")
        webhook.exit("ZUL0001", "2025-01-01T13:00:00.000Z")

        webhook.exit("ZUL0001", "2025-01-01T18:00:00.000Z").andExpect { status { isOk() } }

        val session = sessions("ZUL0001").single()
        assertEquals(Instant.parse("2025-01-01T13:00:00Z"), session.exitTime)
        assertEquals(BigDecimal("9.00"), session.amount)
        assertEquals(listOf(AnomalyType.EXIT_WITHOUT_ACTIVE_SESSION), anomalyTypes())
    }

    @Test
    fun `EXIT anterior ao ENTRY encerra sem cobranca, libera a vaga e registra anomalia`() {
        webhook.entry("ZUL0001", "2025-01-01T12:00:00.000Z")
        webhook.parked("ZUL0001", "-23.561684", "-46.655981")

        webhook.exit("ZUL0001", "2025-01-01T11:00:00.000Z").andExpect { status { isOk() } }

        val session = sessions("ZUL0001").single()
        assertEquals(SessionStatus.EXITED, session.status)
        assertEquals(BigDecimal("0.00"), session.amount)
        assertFalse(spotRepository.findById(1).orElseThrow().occupied)
        assertEquals(listOf(AnomalyType.EXIT_BEFORE_ENTRY), anomalyTypes())
    }

    @Test
    fun `EXIT sem horario responde 400 apontando o campo`() {
        webhook.post("""{"license_plate": "ZUL0001", "event_type": "EXIT"}""").andExpect {
            status { isBadRequest() }
            jsonPath("$.detail") { value("Campo ausente ou inválido: exit_time") }
        }
    }
}
