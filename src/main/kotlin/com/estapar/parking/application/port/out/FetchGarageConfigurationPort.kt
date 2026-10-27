package com.estapar.parking.application.port.out

import com.estapar.parking.domain.model.GarageConfiguration

/** Fonte externa da configuração da garagem (o simulador). */
interface FetchGarageConfigurationPort {
    fun fetch(): GarageConfiguration
}
