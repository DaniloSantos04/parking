package com.estapar.parking.domain.model

import com.estapar.parking.domain.pricing.FeeCalculator
import java.math.BigDecimal
import java.time.Instant

/**
 * Permanência de um veículo na garagem, do ENTRY ao EXIT.
 *
 * Enquanto não está [SessionStatus.EXITED], a sessão está ativa e ocupa uma posição na lotação.
 * As transições válidas são ENTERED → PARKED → EXITED e ENTERED → EXITED (carro que saiu sem
 * um PARKED válido); qualquer outra é erro de programação.
 */
data class ParkingSession(
    val id: Long? = null,
    val licensePlate: String,
    val status: SessionStatus,
    val entryTime: Instant,
    /** Multiplicador do preço dinâmico, travado no ENTRY. */
    val priceMultiplier: BigDecimal,
    /** Vaga e setor, definidos no PARKED. */
    val spotId: Long? = null,
    val sectorCode: String? = null,
    /** Preço-base por hora: o do setor, definido no PARKED; ou o usado na cobrança de quem saiu sem PARKED. */
    val basePrice: BigDecimal? = null,
    /** Definidos no EXIT. */
    val exitTime: Instant? = null,
    val amount: BigDecimal? = null,
) {
    val isActive: Boolean get() = status != SessionStatus.EXITED

    /** ENTERED → PARKED: o carro estacionou em [spot], cujo setor cobra [basePrice] por hora. */
    fun park(spot: Spot, basePrice: BigDecimal): ParkingSession {
        check(status == SessionStatus.ENTERED) { "Sessão de $licensePlate não pode estacionar no status $status" }
        return copy(status = SessionStatus.PARKED, spotId = spot.id, sectorCode = spot.sectorCode, basePrice = basePrice)
    }

    /**
     * PARKED → EXITED, cobrando pelo preço-base do setor.
     * ENTERED → EXITED, cobrando por [fallbackBasePrice], porque sem PARKED não há setor.
     *
     * Saída anterior à entrada encerra a sessão sem cobrança; quem chama registra a anomalia.
     */
    fun exit(exitTime: Instant, fallbackBasePrice: BigDecimal): ParkingSession {
        check(isActive) { "Sessão de $licensePlate já foi encerrada" }
        val chargedBasePrice = basePrice ?: fallbackBasePrice
        val amount = if (exitTime.isBefore(entryTime)) {
            BigDecimal.ZERO.setScale(2)
        } else {
            FeeCalculator.calculate(entryTime, exitTime, chargedBasePrice, priceMultiplier)
        }
        return copy(status = SessionStatus.EXITED, basePrice = chargedBasePrice, exitTime = exitTime, amount = amount)
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
