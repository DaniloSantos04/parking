package com.estapar.parking.application.port.out

import com.estapar.parking.domain.model.GarageConfiguration

interface SaveGarageConfigurationPort {
    fun save(configuration: GarageConfiguration)
}
