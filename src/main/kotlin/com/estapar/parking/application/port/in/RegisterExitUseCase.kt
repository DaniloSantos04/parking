package com.estapar.parking.application.port.`in`

import java.time.Instant

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
