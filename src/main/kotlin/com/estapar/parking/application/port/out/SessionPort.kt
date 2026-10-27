package com.estapar.parking.application.port.out

import com.estapar.parking.domain.model.ParkingSession

interface SessionPort {
    /** Sessão ainda não encerrada da placa, se houver (no máximo uma). */
    fun findActive(licensePlate: String): ParkingSession?

    /** Quantidade de sessões ativas: a lotação atual da garagem. */
    fun countActive(): Int

    fun save(session: ParkingSession): ParkingSession
}
