package com.estapar.parking.adapter.out.persistence.repository

import com.estapar.parking.adapter.out.persistence.entity.GarageEntity
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query

interface GarageJpaRepository : JpaRepository<GarageEntity, Int> {

    /** SELECT ... FOR UPDATE: segura a linha até o fim da transação. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from GarageEntity g where g.id = :id")
    fun findByIdForUpdate(id: Int): GarageEntity?
}
