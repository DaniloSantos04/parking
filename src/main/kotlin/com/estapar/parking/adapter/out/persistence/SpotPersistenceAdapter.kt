package com.estapar.parking.adapter.out.persistence

import com.estapar.parking.adapter.out.persistence.entity.SpotEntity
import com.estapar.parking.adapter.out.persistence.repository.SpotJpaRepository
import com.estapar.parking.application.port.out.SpotPort
import com.estapar.parking.domain.model.Coordinates
import com.estapar.parking.domain.model.Spot
import org.springframework.stereotype.Component

@Component
class SpotPersistenceAdapter(
    private val repository: SpotJpaRepository,
) : SpotPort {

    override fun findByCoordinates(coordinates: Coordinates): Spot? =
        repository.findByLatAndLng(coordinates.lat, coordinates.lng)?.toDomain()

    override fun isOccupied(spotId: Long): Boolean = spot(spotId).occupied

    override fun markOccupied(spotId: Long) {
        repository.save(spot(spotId).apply { occupied = true })
    }

    override fun release(spotId: Long) {
        repository.save(spot(spotId).apply { occupied = false })
    }

    private fun spot(spotId: Long): SpotEntity =
        repository.findById(spotId).orElseThrow { IllegalStateException("Vaga $spotId não encontrada") }

    private fun SpotEntity.toDomain() = Spot(id = id, sectorCode = sectorCode, coordinates = Coordinates.of(lat, lng))
}
