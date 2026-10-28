package com.estapar.parking.application.port.out

import java.math.BigDecimal
import java.time.Instant

interface RevenuePort {
    /**
     * Soma o valor das sessões encerradas com saída em [from, to); só do [sectorCode] se informado.
     * Sessões sem setor (saída sem PARKED) entram apenas na soma sem filtro de setor.
     */
    fun sumExitedAmount(from: Instant, to: Instant, sectorCode: String?): BigDecimal
}
