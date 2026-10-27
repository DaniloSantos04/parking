package com.estapar.parking.adapter.out.persistence

import com.estapar.parking.adapter.out.persistence.entity.AnomalyEntity
import com.estapar.parking.adapter.out.persistence.repository.AnomalyJpaRepository
import com.estapar.parking.application.port.out.AnomalyPort
import com.estapar.parking.domain.model.Anomaly
import org.springframework.stereotype.Component

@Component
class AnomalyPersistenceAdapter(
    private val repository: AnomalyJpaRepository,
) : AnomalyPort {

    override fun save(anomaly: Anomaly) {
        repository.save(
            AnomalyEntity(
                type = anomaly.type,
                licensePlate = anomaly.licensePlate,
                detail = anomaly.detail.take(MAX_DETAIL_LENGTH),
                occurredAt = anomaly.occurredAt,
            ),
        )
    }

    private companion object {
        const val MAX_DETAIL_LENGTH = 500
    }
}
