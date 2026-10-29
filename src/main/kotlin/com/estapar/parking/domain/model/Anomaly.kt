package com.estapar.parking.domain.model

import java.time.Instant

data class Anomaly(
    val type: AnomalyType,
    val licensePlate: String?,
    val detail: String,
    val occurredAt: Instant,
)

enum class AnomalyType {
    GARAGE_FULL,

    DUPLICATE_ENTRY,

    PARKED_WITHOUT_ENTRY,

    ALREADY_PARKED,

    SPOT_NOT_FOUND,

    SPOT_OCCUPIED,

    EXIT_WITHOUT_ACTIVE_SESSION,

    EXIT_WITHOUT_PARKED,

    EXIT_BEFORE_ENTRY,
}
