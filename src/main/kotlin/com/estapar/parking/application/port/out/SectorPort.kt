package com.estapar.parking.application.port.out

import com.estapar.parking.domain.model.Sector
import java.math.BigDecimal

interface SectorPort {
    fun findByCode(code: String): Sector?

    /** Menor preço-base entre os setores; usado para cobrar quem saiu sem PARKED. */
    fun findLowestBasePrice(): BigDecimal?
}
