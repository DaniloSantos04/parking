package com.estapar.parking.application.port.out

import com.estapar.parking.domain.model.GarageConfiguration

interface FetchGarageConfigurationPort {
    fun fetch(): GarageConfiguration
}
