package com.estapar.parking.application.port.`in`

import com.estapar.parking.domain.model.GarageConfiguration

/** Busca a configuração da garagem na fonte externa e a grava. Pode ser chamado mais de uma vez. */
interface LoadGarageUseCase {
    fun load(): GarageConfiguration
}
