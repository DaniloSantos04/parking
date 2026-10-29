package com.estapar.parking.adapter.out.persistence.repository

import com.estapar.parking.adapter.out.persistence.entity.ParkingSessionEntity
import com.estapar.parking.domain.model.SessionStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.math.BigDecimal
import java.time.Instant

interface ParkingSessionJpaRepository : JpaRepository<ParkingSessionEntity, Long> {

    fun findByLicensePlateAndStatusIn(licensePlate: String, statuses: Collection<SessionStatus>): ParkingSessionEntity?

    fun countByStatusIn(statuses: Collection<SessionStatus>): Long

    @Query(
        """
        select coalesce(sum(s.amount), 0) from ParkingSessionEntity s
        where s.status = :status
          and s.exitTime >= :from and s.exitTime < :to
          and (:sectorCode is null or s.sectorCode = :sectorCode)
        """,
    )
    fun sumAmount(status: SessionStatus, from: Instant, to: Instant, sectorCode: String?): BigDecimal
}
