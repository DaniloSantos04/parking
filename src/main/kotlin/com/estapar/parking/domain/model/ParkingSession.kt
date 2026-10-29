package com.estapar.parking.domain.model

import com.estapar.parking.domain.pricing.FeeCalculator
import java.math.BigDecimal
import java.time.Instant

data class ParkingSession(
    val id: Long? = null,
    val licensePlate: String,
    val status: SessionStatus,
    val entryTime: Instant,
    val priceMultiplier: BigDecimal,
    val spotId: Long? = null,
    val sectorCode: String? = null,
    val basePrice: BigDecimal? = null,
    val exitTime: Instant? = null,
    val amount: BigDecimal? = null,
) {
    val isActive: Boolean get() = status != SessionStatus.EXITED

    fun park(spot: Spot, basePrice: BigDecimal): ParkingSession {
        check(status == SessionStatus.ENTERED) { "Sessão de $licensePlate não pode estacionar no status $status" }
        return copy(status = SessionStatus.PARKED, spotId = spot.id, sectorCode = spot.sectorCode, basePrice = basePrice)
    }

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
