package com.estapar.parking.domain.pricing

import java.math.BigDecimal

/** Decide se um carro pode entrar e, se puder, o multiplicador de preço travado para a permanência. */
interface PricingPolicy {
    fun priceEntry(occupancy: Occupancy): EntryPricing
}

sealed interface EntryPricing {
    /** Entrada liberada; o preço-base da hora será multiplicado por [multiplier]. */
    data class Allowed(val multiplier: BigDecimal) : EntryPricing

    /** Garagem 100% ocupada: a cancela fica fechada até uma saída liberar vaga. */
    data object GarageFull : EntryPricing
}
