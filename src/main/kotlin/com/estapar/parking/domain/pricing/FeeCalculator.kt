package com.estapar.parking.domain.pricing

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.Instant

/**
 * Valor a pagar por uma permanência, conforme o enunciado:
 * - até [FREE_PERIOD] (inclusive), não paga nada;
 * - acima disso, paga por hora cheia, arredondada para cima e contando desde a primeira hora:
 *   `horas × preço-base × multiplicador`.
 *
 * As horas são arredondadas antes de aplicar o multiplicador, e só o valor final é arredondado
 * para centavos (HALF_UP).
 */
object FeeCalculator {

    val FREE_PERIOD: Duration = Duration.ofMinutes(30)
    private val ONE_HOUR: Duration = Duration.ofHours(1)
    private const val MONEY_SCALE = 2

    fun calculate(entryTime: Instant, exitTime: Instant, basePrice: BigDecimal, multiplier: BigDecimal): BigDecimal {
        require(!exitTime.isBefore(entryTime)) { "Saída ($exitTime) anterior à entrada ($entryTime)" }

        val stay = Duration.between(entryTime, exitTime)
        if (stay <= FREE_PERIOD) return BigDecimal.ZERO.setScale(MONEY_SCALE)

        return basePrice
            .multiply(BigDecimal.valueOf(chargeableHours(stay)))
            .multiply(multiplier)
            .setScale(MONEY_SCALE, RoundingMode.HALF_UP)
    }

    /** Horas iniciadas: 1h exata conta 1, 1h e 1 segundo conta 2. */
    private fun chargeableHours(stay: Duration): Long {
        val fullHours = stay.dividedBy(ONE_HOUR)
        val startedAnotherHour = stay > ONE_HOUR.multipliedBy(fullHours)
        return if (startedAnotherHour) fullHours + 1 else fullHours
    }
}
