package com.estapar.parking.domain.model

import java.math.BigDecimal
import java.math.RoundingMode

@ConsistentCopyVisibility
data class Coordinates private constructor(val lat: BigDecimal, val lng: BigDecimal) {

    companion object {
        const val SCALE = 6

        fun of(lat: BigDecimal, lng: BigDecimal): Coordinates =
            Coordinates(lat.setScale(SCALE, RoundingMode.HALF_UP), lng.setScale(SCALE, RoundingMode.HALF_UP))
    }
}
