package com.estapar.parking.adapter.out.persistence.repository

import com.estapar.parking.adapter.out.persistence.entity.SpotEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.math.BigDecimal

interface SpotJpaRepository : JpaRepository<SpotEntity, Long> {

    fun findByLatAndLng(lat: BigDecimal, lng: BigDecimal): SpotEntity?
}
