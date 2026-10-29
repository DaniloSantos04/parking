package com.estapar.parking.application.port.`in`

import com.estapar.parking.domain.model.Coordinates

interface RegisterParkedUseCase {
    fun registerParked(command: ParkedCommand)
}

@JvmRecord
data class ParkedCommand(
    val licensePlate: String,
    val coordinates: Coordinates,
) {
    init {
        require(licensePlate.isNotBlank()) { "A placa não pode ser vazia" }
    }
}
