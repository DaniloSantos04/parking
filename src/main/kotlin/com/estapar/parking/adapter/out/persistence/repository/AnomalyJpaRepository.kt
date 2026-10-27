package com.estapar.parking.adapter.out.persistence.repository

import com.estapar.parking.adapter.out.persistence.entity.AnomalyEntity
import org.springframework.data.jpa.repository.JpaRepository

interface AnomalyJpaRepository : JpaRepository<AnomalyEntity, Long>
