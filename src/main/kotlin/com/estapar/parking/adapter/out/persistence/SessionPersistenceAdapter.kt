package com.estapar.parking.adapter.out.persistence

import com.estapar.parking.adapter.out.persistence.entity.ParkingSessionEntity
import com.estapar.parking.adapter.out.persistence.repository.ParkingSessionJpaRepository
import com.estapar.parking.application.port.out.SessionPort
import com.estapar.parking.domain.model.ParkingSession
import com.estapar.parking.domain.model.SessionStatus
import org.springframework.stereotype.Component

@Component
class SessionPersistenceAdapter(
    private val repository: ParkingSessionJpaRepository,
) : SessionPort {

    override fun findActive(licensePlate: String): ParkingSession? =
        repository.findByLicensePlateAndStatusIn(licensePlate, ACTIVE_STATUSES)?.toDomain()

    override fun countActive(): Int = repository.countByStatusIn(ACTIVE_STATUSES).toInt()

    override fun save(session: ParkingSession): ParkingSession = repository.save(session.toEntity()).toDomain()

    private fun ParkingSession.toEntity() = ParkingSessionEntity(
        id = id,
        licensePlate = licensePlate,
        status = status,
        entryTime = entryTime,
        priceMultiplier = priceMultiplier,
        spotId = spotId,
        sectorCode = sectorCode,
        basePrice = basePrice,
    )

    private fun ParkingSessionEntity.toDomain() = ParkingSession(
        id = id,
        licensePlate = licensePlate,
        status = status,
        entryTime = entryTime,
        priceMultiplier = priceMultiplier,
        spotId = spotId,
        sectorCode = sectorCode,
        basePrice = basePrice,
    )

    private companion object {
        val ACTIVE_STATUSES = SessionStatus.entries.filter { it != SessionStatus.EXITED }
    }
}
