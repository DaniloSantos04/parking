package com.estapar.parking.adapter.out.persistence.repository

import com.estapar.parking.adapter.out.persistence.entity.SpotEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.math.BigDecimal

interface SpotJpaRepository : JpaRepository<SpotEntity, Long> {

    /** Usa o índice único (lat, lng). As coordenadas chegam normalizadas para 6 casas. */
    fun findByLatAndLng(lat: BigDecimal, lng: BigDecimal): SpotEntity?
}
