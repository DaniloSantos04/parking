package com.estapar.parking.adapter.out.persistence.repository

import com.estapar.parking.adapter.out.persistence.entity.SectorEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.math.BigDecimal

interface SectorJpaRepository : JpaRepository<SectorEntity, String> {

    @Query("select min(s.basePrice) from SectorEntity s")
    fun findLowestBasePrice(): BigDecimal?
}
