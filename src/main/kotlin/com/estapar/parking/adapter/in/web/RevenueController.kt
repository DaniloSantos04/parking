package com.estapar.parking.adapter.`in`.web

import com.estapar.parking.adapter.`in`.web.dto.RevenueResponse
import com.estapar.parking.application.port.`in`.GetRevenueUseCase
import com.estapar.parking.application.port.`in`.RevenueQuery
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

/**
 * GET /revenue?date=2025-01-01&sector=A. Os dois parâmetros são opcionais: sem `date`, o dia atual
 * (São Paulo); sem `sector`, a garagem inteira.
 */
@RestController
class RevenueController(
    private val getRevenue: GetRevenueUseCase,
) {

    @GetMapping("/revenue")
    fun revenue(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate?,
        @RequestParam(required = false) sector: String?,
    ): RevenueResponse {
        val sectorCode = sector?.trim()?.uppercase()?.takeIf { it.isNotEmpty() }
        val result = getRevenue.getRevenue(RevenueQuery(date = date, sectorCode = sectorCode))
        return RevenueResponse(amount = result.amount, currency = result.currency, timestamp = result.timestamp)
    }
}
