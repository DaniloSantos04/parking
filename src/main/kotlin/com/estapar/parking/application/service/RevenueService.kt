package com.estapar.parking.application.service

import com.estapar.parking.application.port.`in`.GetRevenueUseCase
import com.estapar.parking.application.port.`in`.RevenueQuery
import com.estapar.parking.application.port.`in`.RevenueResult
import com.estapar.parking.application.port.`in`.SectorNotFoundException
import com.estapar.parking.application.port.out.RevenuePort
import com.estapar.parking.application.port.out.SectorPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Clock
import java.time.ZoneId

/**
 * A receita entra no dia da **saída**, no fuso de São Paulo, e conta só as saídas já ocorridas
 * ("até o momento"): para o dia atual, o intervalo termina agora; para dias passados, à meia-noite.
 */
@Service
class RevenueService(
    private val revenuePort: RevenuePort,
    private val sectors: SectorPort,
    private val clock: Clock,
) : GetRevenueUseCase {

    @Transactional(readOnly = true)
    override fun getRevenue(query: RevenueQuery): RevenueResult {
        query.sectorCode?.let { if (sectors.findByCode(it) == null) throw SectorNotFoundException(it) }

        val now = clock.instant()
        val day = query.date ?: now.atZone(BUSINESS_ZONE).toLocalDate()
        val dayStart = day.atStartOfDay(BUSINESS_ZONE).toInstant()
        val dayEnd = day.plusDays(1).atStartOfDay(BUSINESS_ZONE).toInstant()
        val until = minOf(dayEnd, now)

        val amount = if (until.isAfter(dayStart)) {
            revenuePort.sumExitedAmount(dayStart, until, query.sectorCode)
        } else {
            BigDecimal.ZERO
        }
        return RevenueResult(amount = amount.setScale(2, RoundingMode.HALF_UP), currency = CURRENCY, timestamp = now)
    }

    companion object {
        val BUSINESS_ZONE: ZoneId = ZoneId.of("America/Sao_Paulo")
        const val CURRENCY = "BRL"
    }
}
