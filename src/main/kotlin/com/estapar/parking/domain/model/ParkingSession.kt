package com.estapar.parking.domain.model

import java.math.BigDecimal
import java.time.Instant

/**
 * Permanência de um veículo na garagem, do ENTRY ao EXIT.
 *
 * Enquanto não está [SessionStatus.EXITED], a sessão está ativa e ocupa uma posição na lotação.
 */
data class ParkingSession(
    val id: Long? = null,
    val licensePlate: String,
    val status: SessionStatus,
    val entryTime: Instant,
    /** Multiplicador do preço dinâmico, travado no ENTRY. */
    val priceMultiplier: BigDecimal,
) {
    val isActive: Boolean get() = status != SessionStatus.EXITED

    companion object {
        fun enter(licensePlate: String, entryTime: Instant, priceMultiplier: BigDecimal) = ParkingSession(
            licensePlate = licensePlate,
            status = SessionStatus.ENTERED,
            entryTime = entryTime,
            priceMultiplier = priceMultiplier,
        )
    }
}
