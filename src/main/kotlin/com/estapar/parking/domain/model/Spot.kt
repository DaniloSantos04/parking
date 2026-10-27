package com.estapar.parking.domain.model

/** Vaga física de um setor, identificada pelo simulador e localizada por coordenadas. */
data class Spot(
    val id: Long,
    val sectorCode: String,
    val coordinates: Coordinates,
)
