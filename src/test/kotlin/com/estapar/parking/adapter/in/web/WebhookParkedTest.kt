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
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@IntegrationTest
class WebhookParkedTest(
    @Autowired mockMvc: MockMvc,
    @Autowired private val sessionRepository: ParkingSessionJpaRepository,
    @Autowired private val anomalyRepository: AnomalyJpaRepository,
    @Autowired private val spotRepository: SpotJpaRepository,
    @Autowired private val databaseCleaner: DatabaseCleaner,
) {
    private val webhook = WebhookClient(mockMvc)

    @BeforeEach
    fun cleanDatabase() = databaseCleaner.clean()

    private fun session(plate: String) = sessionRepository.findAll().single { it.licensePlate == plate }

    private fun spotOccupied(id: Long) = spotRepository.findById(id).orElseThrow().occupied

    private fun anomalyTypes() = anomalyRepository.findAll().map { it.type }

    @Test
    fun `estaciona na vaga das coordenadas, grava setor e preco-base e ocupa a vaga`() {
        webhook.entry("ZUL0001")

        webhook.parked("ZUL0001", "-23.561644", "-46.655941").andExpect { status { isOk() } }

        val session = session("ZUL0001")
        assertEquals(SessionStatus.PARKED, session.status)
        assertEquals(3L, session.spotId)
        assertEquals("B", session.sectorCode)
        assertEquals(BigDecimal("4.10"), session.basePrice)
        assertTrue(spotOccupied(3))
        assertEquals(emptyList(), anomalyTypes())
    }

    @Test
    fun `encontra a vaga mesmo com outra escala decimal nas coordenadas`() {
        webhook.entry("ZUL0001")

        webhook.parked("ZUL0001", "-23.5616440", "-46.65594100").andExpect { status { isOk() } }

        assertEquals(3L, session("ZUL0001").spotId)
    }

    @Test
    fun `PARKED sem ENTRY responde 200 e registra anomalia`() {
        webhook.parked("ZUL0001", "-23.561644", "-46.655941").andExpect { status { isOk() } }

        assertEquals(0, sessionRepository.count())
        assertFalse(spotOccupied(3))
        assertEquals(listOf(AnomalyType.PARKED_WITHOUT_ENTRY), anomalyTypes())
    }

    @Test
    fun `PARKED repetido responde 200, mantem a vaga original e registra anomalia`() {
        webhook.entry("ZUL0001")
        webhook.parked("ZUL0001", "-23.561684", "-46.655981")

        webhook.parked("ZUL0001", "-23.561644", "-46.655941").andExpect { status { isOk() } }

        assertEquals(1L, session("ZUL0001").spotId)
        assertFalse(spotOccupied(3))
        assertEquals(listOf(AnomalyType.ALREADY_PARKED), anomalyTypes())
    }

    @Test
    fun `PARKED em coordenadas sem vaga responde 200 e registra anomalia`() {
        webhook.entry("ZUL0001")

        webhook.parked("ZUL0001", "-10.000000", "-20.000000").andExpect { status { isOk() } }

        val session = session("ZUL0001")
        assertEquals(SessionStatus.ENTERED, session.status)
        assertNull(session.spotId)
        assertEquals(listOf(AnomalyType.SPOT_NOT_FOUND), anomalyTypes())
    }

    @Test
    fun `PARKED em vaga ocupada responde 200, nao muda nenhuma sessao e registra anomalia`() {
        webhook.entry("ZUL0001")
        webhook.entry("ZUL0002")
        webhook.parked("ZUL0001", "-23.561684", "-46.655981")

        webhook.parked("ZUL0002", "-23.561684", "-46.655981").andExpect { status { isOk() } }

        assertEquals(1L, session("ZUL0001").spotId)
        assertEquals(SessionStatus.ENTERED, session("ZUL0002").status)
        assertNull(session("ZUL0002").spotId)
        assertEquals(listOf(AnomalyType.SPOT_OCCUPIED), anomalyTypes())
    }

    @Test
    fun `PARKED no formato real do simulador, com lat antes da placa`() {
        webhook.entry("SKX77845")

        webhook.post("""{"lat":-23.561604,"lng":-46.655901,"license_plate":"SKX77845","event_type":"PARKED"}""")
            .andExpect { status { isOk() } }

        assertEquals(SessionStatus.PARKED, session("SKX77845").status)
        assertEquals(5L, session("SKX77845").spotId)
    }

    @Test
    fun `PARKED sem coordenadas responde 400 apontando o campo`() {
        webhook.post("""{"license_plate": "ZUL0001", "lng": -46.655981, "event_type": "PARKED"}""").andExpect {
            status { isBadRequest() }
            jsonPath("$.detail") { value("Campo ausente ou inválido: lat") }
        }
    }

    @Test
    fun `PARKED com latitude fora de -90 a 90 responde 400`() {
        webhook.parked("ZUL0001", "91", "-46.655981").andExpect {
            status { isBadRequest() }
            jsonPath("$.detail") { value(org.hamcrest.Matchers.startsWith("lat:")) }
        }
    }

    @Test
    fun `PARKED com longitude fora de -180 a 180 responde 400`() {
        webhook.parked("ZUL0001", "-23.561684", "-180.000001").andExpect { status { isBadRequest() } }
    }
}
