package com.estapar.parking.application.port.out

import com.estapar.parking.domain.model.Coordinates
import com.estapar.parking.domain.model.Spot

interface SpotPort {
    fun findByCoordinates(coordinates: Coordinates): Spot?

    fun isOccupied(spotId: Long): Boolean

    fun markOccupied(spotId: Long)
}
