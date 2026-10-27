package com.estapar.parking.adapter.out.simulator.dto

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal

/**
 * Corpo do GET /garage do simulador. Campos não usados pelo desafio (open_hour, close_hour,
 * duration_limit_minutes, occupied) são ignorados.
 */
@JvmRecord
data class GarageResponse(
    val garage: List<SectorResponse>,
    val spots: List<SpotResponse>,
)

@JvmRecord
data class SectorResponse(
    val sector: String,
    // O simulador envia base_price; o exemplo do enunciado usa basePrice. Os dois são aceitos.
    @JsonProperty("base_price") @JsonAlias("basePrice")
    val basePrice: BigDecimal,
    @JsonProperty("max_capacity")
    val maxCapacity: Int,
)

@JvmRecord
data class SpotResponse(
    val id: Long,
    val sector: String,
    val lat: BigDecimal,
    val lng: BigDecimal,
)
