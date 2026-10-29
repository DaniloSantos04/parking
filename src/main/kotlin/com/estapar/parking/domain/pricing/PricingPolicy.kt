package com.estapar.parking.domain.pricing

import java.math.BigDecimal

interface PricingPolicy {
    fun priceEntry(occupancy: Occupancy): EntryPricing
}

sealed interface EntryPricing {
    data class Allowed(val multiplier: BigDecimal) : EntryPricing

    data object GarageFull : EntryPricing
}
