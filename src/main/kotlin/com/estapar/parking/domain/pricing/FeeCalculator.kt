package com.estapar.parking.domain.pricing

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.Instant

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

    private fun chargeableHours(stay: Duration): Long {
        val fullHours = stay.dividedBy(ONE_HOUR)
        val startedAnotherHour = stay > ONE_HOUR.multipliedBy(fullHours)
        return if (startedAnotherHour) fullHours + 1 else fullHours
    }
}
