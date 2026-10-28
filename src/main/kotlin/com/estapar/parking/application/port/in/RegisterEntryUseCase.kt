package com.estapar.parking.application.port.`in`

import java.time.Instant

/**
 * Processa a chegada de um veículo à cancela. Recusas (garagem cheia, placa já dentro)
 * não são erros: viram anomalias registradas.
 */
interface RegisterEntryUseCase {
    fun registerEntry(command: EntryCommand)
}

@JvmRecord
data class EntryCommand(
    val licensePlate: String,
    val entryTime: Instant,
) {
    init {
        require(licensePlate.isNotBlank()) { "A placa não pode ser vazia" }
    }
}
