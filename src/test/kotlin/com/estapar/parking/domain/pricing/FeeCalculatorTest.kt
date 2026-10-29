package com.estapar.parking.domain.pricing

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals

class FeeCalculatorTest {

    private val entry = Instant.parse("2025-01-01T12:00:00Z")

    private fun feeFor(stay: Duration, basePrice: String = "10.00", multiplier: String = "1.00") =
        FeeCalculator.calculate(entry, entry.plus(stay), BigDecimal(basePrice), BigDecimal(multiplier))

    @ParameterizedTest(name = "permanência {0} → R$ {1}")
    @CsvSource(
        "PT0S, 0.00",
        "PT29M59S, 0.00",
        "PT30M, 0.00",
        "PT30M1S, 10.00",
        "PT30M0.001S, 10.00",
        "PT59M, 10.00",
        "PT1H, 10.00",
        "PT1H0M1S, 20.00",
        "PT1H59M, 20.00",
        "PT2H, 20.00",
        "PT2H1S, 30.00",
        "PT24H, 240.00",
    )
    fun `cobra horas cheias arredondadas para cima depois dos 30 minutos gratis`(stay: Duration, expected: BigDecimal) {
        assertEquals(expected, feeFor(stay))
    }

    @ParameterizedTest(name = "R$ {0} × {1}h × {2} → R$ {3}")
    @CsvSource(
        "10.00, 1, 0.90, 9.00",
        "10.00, 1, 1.10, 11.00",
        "10.00, 1, 1.25, 12.50",
        "40.50, 1, 0.90, 36.45",
        "40.50, 1, 1.10, 44.55",
        "40.50, 1, 1.25, 50.63",
        "4.10, 1, 0.90, 3.69",
        "4.10, 3, 1.25, 15.38",
    )
    fun `aplica o multiplicador travado na entrada e arredonda centavos com HALF_UP`(
        basePrice: String,
        hours: Long,
        multiplier: String,
        expected: BigDecimal,
    ) {
        assertEquals(expected, feeFor(Duration.ofHours(hours), basePrice, multiplier))
    }

    @Test
    fun `arredonda so o valor final, nao o preco de cada hora`() {
        assertEquals(BigDecimal("15.38"), feeFor(Duration.ofHours(3), "4.10", "1.25"))
    }

    @Test
    fun `arredonda as horas antes de aplicar o multiplicador`() {
        assertEquals(BigDecimal("22.00"), feeFor(Duration.ofMinutes(61), "10.00", "1.10"))
    }

    @Test
    fun `valor sempre tem 2 casas decimais`() {
        assertEquals(2, feeFor(Duration.ZERO).scale())
        assertEquals(2, feeFor(Duration.ofHours(1), "10", "1").scale())
    }

    @Test
    fun `rejeita saida anterior a entrada`() {
        assertThrows<IllegalArgumentException> {
            FeeCalculator.calculate(entry, entry.minusSeconds(1), BigDecimal.TEN, BigDecimal.ONE)
        }
    }
}
