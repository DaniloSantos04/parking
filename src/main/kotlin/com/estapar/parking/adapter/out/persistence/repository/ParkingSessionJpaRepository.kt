package com.estapar.parking.adapter.out.persistence.repository

import com.estapar.parking.adapter.out.persistence.entity.ParkingSessionEntity
import com.estapar.parking.domain.model.SessionStatus
import org.springframework.data.jpa.repository.JpaRepository

interface ParkingSessionJpaRepository : JpaRepository<ParkingSessionEntity, Long> {

    fun findByLicensePlateAndStatusIn(licensePlate: String, statuses: Collection<SessionStatus>): ParkingSessionEntity?

    fun countByStatusIn(statuses: Collection<SessionStatus>): Long
}
