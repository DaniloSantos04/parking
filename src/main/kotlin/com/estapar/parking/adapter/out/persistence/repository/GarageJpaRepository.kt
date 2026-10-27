package com.estapar.parking.adapter.out.persistence.repository

import com.estapar.parking.adapter.out.persistence.entity.GarageEntity
import org.springframework.data.jpa.repository.JpaRepository

interface GarageJpaRepository : JpaRepository<GarageEntity, Int>
