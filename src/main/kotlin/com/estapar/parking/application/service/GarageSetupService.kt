package com.estapar.parking.application.service

import com.estapar.parking.application.port.`in`.LoadGarageUseCase
import com.estapar.parking.application.port.out.FetchGarageConfigurationPort
import com.estapar.parking.application.port.out.SaveGarageConfigurationPort
import com.estapar.parking.domain.model.GarageConfiguration
import org.springframework.stereotype.Service

@Service
class GarageSetupService(
    private val fetchGarageConfiguration: FetchGarageConfigurationPort,
    private val saveGarageConfiguration: SaveGarageConfigurationPort,
) : LoadGarageUseCase {

    override fun load(): GarageConfiguration {
        val configuration = fetchGarageConfiguration.fetch()
        saveGarageConfiguration.save(configuration)
        return configuration
    }
}
