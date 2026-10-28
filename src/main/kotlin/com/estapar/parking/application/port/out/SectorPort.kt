package com.estapar.parking.application.port.out

import com.estapar.parking.domain.model.Sector

interface SectorPort {
    fun findByCode(code: String): Sector?
}
