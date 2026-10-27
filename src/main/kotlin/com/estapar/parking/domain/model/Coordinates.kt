package com.estapar.parking.domain.model

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Posição de uma vaga. Latitude e longitude são normalizadas para [SCALE] casas decimais,
 * de forma que a mesma posição vinda do GET /garage e do evento PARKED seja sempre igual.
 */
@ConsistentCopyVisibility
data class Coordinates private constructor(val lat: BigDecimal, val lng: BigDecimal) {

    companion object {
        const val SCALE = 6

        fun of(lat: BigDecimal, lng: BigDecimal): Coordinates =
            Coordinates(lat.setScale(SCALE, RoundingMode.HALF_UP), lng.setScale(SCALE, RoundingMode.HALF_UP))
    }
}
