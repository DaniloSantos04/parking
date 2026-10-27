package com.estapar.parking.domain.pricing

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.math.BigDecimal
import kotlin.test.assertEquals

class OccupancyPricingPolicyTest {

    private val policy = OccupancyPricingPolicy()

    @ParameterizedTest(name = "{0} de {1} vagas ocupadas antes da entrada → multiplicador {2}")
    @CsvSource(
        // Garagem de 100 vagas: limites exatos de cada faixa.
        "0, 100, 0.90",
        "24, 100, 0.90",
        "25, 100, 1.00",
        "50, 100, 1.00",
        "51, 100, 1.10",
        "75, 100, 1.10",
        "76, 100, 1.25",
        "99, 100, 1.25",
        // Garagem do simulador (30 vagas): os limites caem entre números inteiros de carros.
        "7, 30, 0.90", // 23,3%
        "8, 30, 1.00", // 26,7%
        "15, 30, 1.00", // 50%
        "16, 30, 1.10", // 53,3%
        "22, 30, 1.10", // 73,3%
        "23, 30, 1.25", // 76,7%
        "29, 30, 1.25", // 96,7%
        // Garagem de 4 vagas: 1 carro é exatamente 25%, 2 é 50% e 3 é 75%.
        "1, 4, 1.00",
        "2, 4, 1.00",
        "3, 4, 1.10",
    )
    fun `aplica a faixa de preco pela lotacao antes da entrada`(occupied: Int, capacity: Int, expected: BigDecimal) {
        assertEquals(EntryPricing.Allowed(expected), policy.priceEntry(Occupancy(occupied, capacity)))
    }

    @ParameterizedTest(name = "{0} de {1} vagas ocupadas → garagem fechada")
    @CsvSource("100, 100", "30, 30", "31, 30", "0, 0")
    fun `fecha a garagem com 100 por cento de lotacao`(occupied: Int, capacity: Int) {
        assertEquals(EntryPricing.GarageFull, policy.priceEntry(Occupancy(occupied, capacity)))
    }

    @Test
    fun `reabre assim que uma saida libera vaga`() {
        assertEquals(EntryPricing.GarageFull, policy.priceEntry(Occupancy(30, 30)))
        assertEquals(EntryPricing.Allowed(BigDecimal("1.25")), policy.priceEntry(Occupancy(29, 30)))
    }

    @Test
    fun `multiplicadores tem escala 2`() {
        listOf(0, 25, 51, 76).forEach { occupied ->
            val pricing = policy.priceEntry(Occupancy(occupied, 100)) as EntryPricing.Allowed
            assertEquals(2, pricing.multiplier.scale())
        }
    }

    @Test
    fun `rejeita ocupacao ou capacidade negativas`() {
        assertThrows<IllegalArgumentException> { Occupancy(-1, 10) }
        assertThrows<IllegalArgumentException> { Occupancy(0, -1) }
    }

    @Test
    fun `nao estoura com capacidades grandes`() {
        val capacity = Int.MAX_VALUE
        assertEquals(EntryPricing.Allowed(BigDecimal("0.90")), policy.priceEntry(Occupancy(capacity / 5, capacity)))
    }
}
