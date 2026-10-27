package com.estapar.parking.adapter.out.persistence

import com.estapar.parking.adapter.out.persistence.entity.GarageEntity
import com.estapar.parking.adapter.out.persistence.repository.GarageJpaRepository
import com.estapar.parking.application.port.out.LockGaragePort
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class GarageLockPersistenceAdapter(
    private val garageRepository: GarageJpaRepository,
) : LockGaragePort {

    /** Exige uma transação aberta pelo chamador: fora dela o lock seria liberado na mesma hora. */
    @Transactional(propagation = Propagation.MANDATORY)
    override fun lockAndGetCapacity(): Int =
        garageRepository.findByIdForUpdate(GarageEntity.SINGLETON_ID)?.capacity
            ?: throw IllegalStateException("Linha da garagem não encontrada; a migration V1 não foi aplicada?")
}
