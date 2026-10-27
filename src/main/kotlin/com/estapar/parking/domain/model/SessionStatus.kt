package com.estapar.parking.domain.model

/** Estados de uma permanência: ENTERED → PARKED → EXITED. */
enum class SessionStatus {
    /** Passou pela cancela; ainda não estacionou numa vaga. */
    ENTERED,

    /** Estacionou numa vaga conhecida; o setor e o preço-base estão definidos. */
    PARKED,

    /** Saiu da garagem; o valor foi calculado. */
    EXITED,
}
