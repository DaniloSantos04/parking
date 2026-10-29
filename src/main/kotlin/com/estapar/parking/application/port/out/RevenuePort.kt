package com.estapar.parking.application.port.out

import java.math.BigDecimal
import java.time.Instant

interface RevenuePort {
    fun sumExitedAmount(from: Instant, to: Instant, sectorCode: String?): BigDecimal
}
