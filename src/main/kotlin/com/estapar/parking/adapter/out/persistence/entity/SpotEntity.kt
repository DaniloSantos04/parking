package com.estapar.parking.adapter.out.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(name = "spot")
class SpotEntity(
    @Id
    val id: Long,

    @Column(name = "sector_code", nullable = false)
    var sectorCode: String,

    @Column(nullable = false, precision = 9, scale = 6)
    var lat: BigDecimal,

    @Column(nullable = false, precision = 9, scale = 6)
    var lng: BigDecimal,

    @Column(nullable = false)
    var occupied: Boolean = false,
)
