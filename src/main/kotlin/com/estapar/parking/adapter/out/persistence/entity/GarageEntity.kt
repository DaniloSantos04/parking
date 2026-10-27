package com.estapar.parking.adapter.out.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "garage")
class GarageEntity(
    @Id
    val id: Int = SINGLETON_ID,

    @Column(nullable = false)
    var capacity: Int,
) {
    companion object {
        /** A garagem é uma linha única, criada pela migration V1. */
        const val SINGLETON_ID = 1
    }
}
