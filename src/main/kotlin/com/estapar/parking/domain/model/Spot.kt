package com.estapar.parking.domain.model

data class Spot(
    val id: Long,
    val sectorCode: String,
    val coordinates: Coordinates,
)
