package com.estapar.parking.domain.pricing

import java.math.BigDecimal

/**
 * Preço dinâmico do enunciado, pela lotação da garagem inteira medida **antes** do carro entrar:
 *
 * | Lotação              | Ajuste |
 * |----------------------|--------|
 * | abaixo de 25%        | −10%   |
 * | de 25% até 50%       | 0%     |
 * | acima de 50% até 75% | +10%   |
 * | acima de 75%         | +25%   |
 * | 100%                 | garagem fechada |
 */
class OccupancyPricingPolicy : PricingPolicy {

    override fun priceEntry(occupancy: Occupancy): EntryPricing = when {
        occupancy.isFull -> EntryPricing.GarageFull
        occupancy.isBelow(25) -> EntryPricing.Allowed(DISCOUNT_10)
        occupancy.isAtMost(50) -> EntryPricing.Allowed(NO_ADJUSTMENT)
        occupancy.isAtMost(75) -> EntryPricing.Allowed(INCREASE_10)
        else -> EntryPricing.Allowed(INCREASE_25)
    }

    companion object {
        val DISCOUNT_10 = BigDecimal("0.90")
        val NO_ADJUSTMENT = BigDecimal("1.00")
        val INCREASE_10 = BigDecimal("1.10")
        val INCREASE_25 = BigDecimal("1.25")
    }
}
