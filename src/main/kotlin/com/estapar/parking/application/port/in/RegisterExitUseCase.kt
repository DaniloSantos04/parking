package com.estapar.parking.application.port.`in`

import java.time.Instant

/**
 * Processa a saída de um veículo: calcula o valor, encerra a sessão e libera a vaga.
 * Inconsistências (placa sem sessão, saída sem PARKED, saída antes da entrada) viram anomalias.
 */
interface RegisterExitUseCase {
    fun registerExit(command: ExitCommand)
}

@JvmRecord
data class ExitCommand(
    val licensePlate: String,
    val exitTime: Instant,
) {
    init {
        require(licensePlate.isNotBlank()) { "A placa não pode ser vazia" }
    }
}
