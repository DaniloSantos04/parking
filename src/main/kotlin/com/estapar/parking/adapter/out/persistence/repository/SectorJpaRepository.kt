package com.estapar.parking.adapter.out.persistence.repository

import com.estapar.parking.adapter.out.persistence.entity.SectorEntity
import org.springframework.data.jpa.repository.JpaRepository

interface SectorJpaRepository : JpaRepository<SectorEntity, String>
