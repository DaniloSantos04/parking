package com.estapar.parking

import com.estapar.parking.adapter.out.persistence.repository.AnomalyJpaRepository
import com.estapar.parking.adapter.out.persistence.repository.ParkingSessionJpaRepository
import com.estapar.parking.adapter.out.persistence.repository.SpotJpaRepository
import org.springframework.stereotype.Component

/**
 * Volta o banco ao estado da subida (garagem carregada, nenhum carro dentro) entre testes de
 * integração, que compartilham o mesmo container.
 */
@Component
class DatabaseCleaner(
    private val sessionRepository: ParkingSessionJpaRepository,
    private val anomalyRepository: AnomalyJpaRepository,
    private val spotRepository: SpotJpaRepository,
) {
    fun clean() {
        sessionRepository.deleteAllInBatch()
        anomalyRepository.deleteAllInBatch()
        spotRepository.saveAll(spotRepository.findAll().onEach { it.occupied = false })
    }
}
