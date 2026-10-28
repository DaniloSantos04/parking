package com.estapar.parking.domain.model

import java.math.BigDecimal
import java.time.Instant

/**
 * Permanência de um veículo na garagem, do ENTRY ao EXIT.
 *
 * Enquanto não está [SessionStatus.EXITED], a sessão está ativa e ocupa uma posição na lotação.
 * As transições válidas são ENTERED → PARKED → EXITED; qualquer outra é erro de programação.
 */
data class ParkingSession(
    val id: Long? = null,
    val licensePlate: String,
    val status: SessionStatus,
    val entryTime: Instant,
    /** Multiplicador do preço dinâmico, travado no ENTRY. */
    val priceMultiplier: BigDecimal,
    /** Vaga, setor e preço-base por hora, definidos no PARKED. */
    val spotId: Long? = null,
    val sectorCode: String? = null,
    val basePrice: BigDecimal? = null,
) {
    val isActive: Boolean get() = status != SessionStatus.EXITED

    /** ENTERED → PARKED: o carro estacionou em [spot], cujo setor cobra [basePrice] por hora. */
    fun park(spot: Spot, basePrice: BigDecimal): ParkingSession {
        check(status == SessionStatus.ENTERED) { "Sessão de $licensePlate não pode estacionar no status $status" }
        return copy(status = SessionStatus.PARKED, spotId = spot.id, sectorCode = spot.sectorCode, basePrice = basePrice)
    }

    companion object {
        fun enter(licensePlate: String, entryTime: Instant, priceMultiplier: BigDecimal) = ParkingSession(
            licensePlate = licensePlate,
            status = SessionStatus.ENTERED,
            entryTime = entryTime,
            priceMultiplier = priceMultiplier,
        )
    }
}
