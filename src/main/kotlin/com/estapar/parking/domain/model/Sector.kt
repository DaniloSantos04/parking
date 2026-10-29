package com.estapar.parking.domain.model

import java.math.BigDecimal

data class Sector(
    val code: String,
    val basePrice: BigDecimal,
    val maxCapacity: Int,
) {
    init {
        require(code.isNotBlank()) { "O código do setor não pode ser vazio" }
        require(basePrice.signum() >= 0) { "O preço-base do setor $code não pode ser negativo" }
        require(maxCapacity >= 0) { "A capacidade do setor $code não pode ser negativa" }
    }
}
