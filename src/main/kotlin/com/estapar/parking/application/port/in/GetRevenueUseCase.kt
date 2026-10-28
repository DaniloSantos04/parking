package com.estapar.parking.application.port.`in`

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

/** Receita das saídas de um dia, da garagem inteira ou de um setor. */
interface GetRevenueUseCase {
    fun getRevenue(query: RevenueQuery): RevenueResult
}

/**
 * @property date dia do faturamento no fuso de São Paulo; `null` é o dia atual.
 * @property sectorCode setor; `null` soma a garagem inteira.
 */
@JvmRecord
data class RevenueQuery(
    val date: LocalDate?,
    val sectorCode: String?,
)

/**
 * @property amount soma dos valores cobrados, com 2 casas.
 * @property timestamp instante em que a receita foi apurada.
 */
@JvmRecord
data class RevenueResult(
    val amount: BigDecimal,
    val currency: String,
    val timestamp: Instant,
)

/** O setor pedido não existe na garagem. */
class SectorNotFoundException(val sectorCode: String) : RuntimeException("Setor não encontrado: $sectorCode")
