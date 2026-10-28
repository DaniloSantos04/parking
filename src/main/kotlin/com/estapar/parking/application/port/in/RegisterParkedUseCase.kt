package com.estapar.parking.application.port.`in`

import com.estapar.parking.domain.model.Coordinates

/**
 * Processa o estacionamento de um veículo numa vaga, identificada pelas coordenadas.
 * Inconsistências (sem ENTRY, vaga inexistente ou ocupada) não são erros: viram anomalias.
 */
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
