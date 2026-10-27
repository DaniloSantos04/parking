package com.estapar.parking.adapter.out.persistence.entity

import com.estapar.parking.domain.model.AnomalyType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "anomaly")
class AnomalyEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    val type: AnomalyType,

    @Column(name = "license_plate", length = 20)
    val licensePlate: String?,

    @Column(nullable = false, length = 500)
    val detail: String,

    @Column(name = "occurred_at", nullable = false)
    val occurredAt: Instant,
)
