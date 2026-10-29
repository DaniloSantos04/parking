package com.estapar.parking.application.port.`in`

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

interface GetRevenueUseCase {
    fun getRevenue(query: RevenueQuery): RevenueResult
}

@JvmRecord
data class RevenueQuery(
    val date: LocalDate?,
    val sectorCode: String?,
)

@JvmRecord
data class RevenueResult(
    val amount: BigDecimal,
    val currency: String,
    val timestamp: Instant,
)

class SectorNotFoundException(val sectorCode: String) : RuntimeException("Setor não encontrado: $sectorCode")
