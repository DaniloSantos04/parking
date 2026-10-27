package com.estapar.parking.adapter.out.persistence.repository

import com.estapar.parking.adapter.out.persistence.entity.SpotEntity
import org.springframework.data.jpa.repository.JpaRepository

interface SpotJpaRepository : JpaRepository<SpotEntity, Long>
