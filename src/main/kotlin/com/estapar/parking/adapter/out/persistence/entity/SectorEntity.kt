package com.estapar.parking.adapter.out.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(name = "sector")
class SectorEntity(
    @Id
    val code: String,

    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    var basePrice: BigDecimal,

    @Column(name = "max_capacity", nullable = false)
    var maxCapacity: Int,
)
