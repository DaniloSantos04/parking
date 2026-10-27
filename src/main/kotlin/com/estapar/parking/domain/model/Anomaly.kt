package com.estapar.parking.domain.model

import java.time.Instant

/** Evento recebido que não pôde ser aplicado. Fica registrado para auditoria. */
data class Anomaly(
    val type: AnomalyType,
    val licensePlate: String?,
    val detail: String,
    val occurredAt: Instant,
)

enum class AnomalyType {
    /** ENTRY recusado: a garagem estava 100% ocupada. */
    GARAGE_FULL,

    /** ENTRY de uma placa que já tem sessão ativa. */
    DUPLICATE_ENTRY,
}
