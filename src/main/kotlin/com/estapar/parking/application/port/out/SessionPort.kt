package com.estapar.parking.application.port.out

import com.estapar.parking.domain.model.ParkingSession

interface SessionPort {
    fun findActive(licensePlate: String): ParkingSession?

    fun countActive(): Int

    fun save(session: ParkingSession): ParkingSession
}
