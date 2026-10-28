package com.estapar.parking.adapter.out.persistence.entity

import com.estapar.parking.domain.model.LicensePlate
import com.estapar.parking.domain.model.SessionStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(name = "parking_session")
class ParkingSessionEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "license_plate", nullable = false, length = LicensePlate.MAX_LENGTH)
    val licensePlate: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    var status: SessionStatus,

    @Column(name = "entry_time", nullable = false)
    val entryTime: Instant,

    @Column(name = "price_multiplier", nullable = false, precision = 4, scale = 2)
    val priceMultiplier: BigDecimal,
)
