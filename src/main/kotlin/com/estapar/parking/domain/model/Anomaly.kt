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

    /** PARKED de uma placa sem sessão ativa (não houve ENTRY, ou o carro já saiu). */
    PARKED_WITHOUT_ENTRY,

    /** PARKED de uma placa que já está estacionada. */
    ALREADY_PARKED,

    /** PARKED com coordenadas que não correspondem a nenhuma vaga da garagem. */
    SPOT_NOT_FOUND,

    /** PARKED numa vaga que já está ocupada por outro carro. */
    SPOT_OCCUPIED,
}
