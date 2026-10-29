package com.estapar.parking.application.port.`in`

import com.estapar.parking.domain.model.GarageConfiguration

interface LoadGarageUseCase {
    fun load(): GarageConfiguration
}
