package com.estapar.parking.domain.model

object LicensePlate {
    const val MAX_LENGTH = 20

    fun normalize(raw: String): String = raw.trim().uppercase()
}
