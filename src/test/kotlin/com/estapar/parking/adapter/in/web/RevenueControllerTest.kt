package com.estapar.parking.adapter.`in`.web

import com.estapar.parking.DatabaseCleaner
import com.estapar.parking.IntegrationTest
import com.estapar.parking.WebhookClient
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.json.JsonCompareMode
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.get

/**
 * GET /revenue ponta a ponta, com receita gerada pelo próprio webhook.
 *
 * Relógio fixo em 2025-01-02T12:00Z (09:00 em São Paulo). Garagem dos testes: vagas 1 e 2 no
 * setor A (R$ 10,00), 3 a 5 no setor B (R$ 4,10). Todos entram com multiplicador 0,90, exceto o
 * CAR0003, que entra com 1,00 (2 de 5 vagas ocupadas, 40%).
 *
 * | Carro   | Setor | Entrada (UTC)    | Saída (UTC)      | Dia da saída (SP) | Valor                     |
 * |---------|-------|------------------|------------------|-------------------|---------------------------|
 * | CAR0001 | A     | 01-01 12:00      | 01-01 14:00      | 01-01             | 2h × 10,00 × 0,90 = 18,00 |
 * | CAR0002 | B     | 01-01 12:00      | 01-01 13:00      | 01-01             | 1h × 4,10 × 0,90 = 3,69   |
 * | CAR0003 | —     | 01-01 12:00      | 01-01 13:00      | 01-01             | 1h × 4,10 × 1,00 = 4,10   |
 * | CAR0004 | A     | 01-01 20:00      | 01-02 02:30      | 01-01 (23:30)     | 7h × 10,00 × 0,90 = 63,00 |
 * | CAR0005 | B     | 01-02 01:00      | 01-02 03:00      | 01-02 (00:00)     | 2h × 4,10 × 0,90 = 7,38   |
 * | CAR0006 | A     | 01-02 10:00      | 01-02 13:00      | depois de "agora" | 3h × 10,00 × 0,90 = 27,00 |
 *
 * CAR0003 saiu sem PARKED: cobrado pelo menor preço-base, sem setor.
 */
@IntegrationTest
class RevenueControllerTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val databaseCleaner: DatabaseCleaner,
) {
    private val webhook = WebhookClient(mockMvc)

    @BeforeEach
    fun seedRevenue() {
        databaseCleaner.clean()
        with(webhook) {
            entry("CAR0001", "2025-01-01T12:00:00.000Z")
            entry("CAR0002", "2025-01-01T12:00:00.000Z")
            entry("CAR0003", "2025-01-01T12:00:00.000Z")
            parked("CAR0001", "-23.561684", "-46.655981")
            parked("CAR0002", "-23.561644", "-46.655941")
            exit("CAR0002", "2025-01-01T13:00:00.000Z")
            exit("CAR0003", "2025-01-01T13:00:00.000Z")
            exit("CAR0001", "2025-01-01T14:00:00.000Z")
            entry("CAR0004", "2025-01-01T20:00:00.000Z")
            parked("CAR0004", "-23.561664", "-46.655961")
            entry("CAR0005", "2025-01-02T01:00:00.000Z")
            parked("CAR0005", "-23.561624", "-46.655921")
            exit("CAR0004", "2025-01-02T02:30:00.000Z")
            exit("CAR0005", "2025-01-02T03:00:00.000Z")
            entry("CAR0006", "2025-01-02T10:00:00.000Z")
            parked("CAR0006", "-23.561684", "-46.655981")
            exit("CAR0006", "2025-01-02T13:00:00.000Z")
        }
    }

    private fun revenue(query: String = ""): ResultActionsDsl = mockMvc.get("/revenue$query")

    private fun ResultActionsDsl.andExpectAmount(amount: Double) = andExpect {
        status { isOk() }
        jsonPath("$.amount") { value(amount) }
    }

    @Test
    fun `responde no formato do enunciado`() {
        revenue("?date=2025-01-01").andExpect {
            status { isOk() }
            content {
                contentType(MediaType.APPLICATION_JSON)
                json("""{"amount":88.79,"currency":"BRL","timestamp":"2025-01-02T12:00:00.000Z"}""", JsonCompareMode.STRICT)
            }
        }
    }

    @Test
    fun `valor zero sai com 2 casas decimais`() {
        revenue("?date=2024-12-31").andExpect {
            status { isOk() }
            content { string("""{"amount":0.00,"currency":"BRL","timestamp":"2025-01-02T12:00:00.000Z"}""") }
        }
    }

    @Test
    fun `sem setor soma a garagem inteira, inclusive saidas sem setor`() {
        revenue("?date=2025-01-01").andExpectAmount(88.79)
    }

    @Test
    fun `por setor soma so as saidas daquele setor`() {
        revenue("?date=2025-01-01&sector=A").andExpectAmount(81.00)
        revenue("?date=2025-01-01&sector=B").andExpectAmount(3.69)
    }

    @Test
    fun `a receita entra no dia da saida no fuso de Sao Paulo`() {
        // CAR0004 saiu 02:30 UTC do dia 2, que ainda é 23:30 do dia 1 em São Paulo.
        revenue("?date=2025-01-01&sector=A").andExpectAmount(81.00)
        // CAR0005 saiu 03:00 UTC do dia 2, que já é 00:00 do dia 2 em São Paulo.
        revenue("?date=2025-01-02&sector=B").andExpectAmount(7.38)
    }

    @Test
    fun `sem data usa o dia atual e soma so as saidas ate o momento`() {
        // Hoje é 2025-01-02 em São Paulo. CAR0006 sai às 13:00 UTC, depois de "agora" (12:00 UTC).
        revenue().andExpectAmount(7.38)
        revenue("?date=2025-01-02").andExpectAmount(7.38)
    }

    @Test
    fun `dia futuro tem receita zero`() {
        revenue("?date=2025-01-03").andExpectAmount(0.00)
    }

    @Test
    fun `setor e normalizado`() {
        mockMvc.get("/revenue") {
            param("date", "2025-01-01")
            param("sector", " a ")
        }.andExpectAmount(81.00)
    }

    @Test
    fun `setor em branco equivale a garagem inteira`() {
        revenue("?date=2025-01-01&sector=").andExpectAmount(88.79)
    }

    @Test
    fun `setor inexistente responde 404`() {
        revenue("?sector=Z").andExpect {
            status { isNotFound() }
            content { contentType(MediaType.APPLICATION_PROBLEM_JSON) }
            jsonPath("$.detail") { value("Setor não encontrado: Z") }
        }
    }

    @Test
    fun `data mal formatada responde 400`() {
        revenue("?date=01/01/2025").andExpect {
            status { isBadRequest() }
            content { contentType(MediaType.APPLICATION_PROBLEM_JSON) }
            jsonPath("$.detail") { value("Parâmetro inválido: date = '01/01/2025' (esperado AAAA-MM-DD)") }
        }
    }

    @Test
    fun `data inexistente responde 400`() {
        revenue("?date=2025-02-30").andExpect { status { isBadRequest() } }
    }
}
