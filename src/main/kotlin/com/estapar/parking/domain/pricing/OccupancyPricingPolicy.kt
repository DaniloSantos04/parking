package com.estapar.parking.domain.pricing

import java.math.BigDecimal

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
