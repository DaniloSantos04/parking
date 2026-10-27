package com.estapar.parking.adapter.out.simulator

import com.estapar.parking.adapter.out.simulator.dto.GarageResponse
import com.estapar.parking.application.port.out.FetchGarageConfigurationPort
import com.estapar.parking.config.SimulatorProperties
import com.estapar.parking.domain.model.Coordinates
import com.estapar.parking.domain.model.GarageConfiguration
import com.estapar.parking.domain.model.Sector
import com.estapar.parking.domain.model.Spot
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

@Component
class SimulatorGarageAdapter(
    restClientBuilder: RestClient.Builder,
    properties: SimulatorProperties,
) : FetchGarageConfigurationPort {

    private val restClient = restClientBuilder.baseUrl(properties.baseUrl).build()

    override fun fetch(): GarageConfiguration {
        val response = restClient.get()
            .uri("/garage")
            .retrieve()
            .body<GarageResponse>()
            ?: throw IllegalStateException("O simulador respondeu GET /garage sem corpo")
        return response.toDomain()
    }

    private fun GarageResponse.toDomain() = GarageConfiguration(
        sectors = garage.map { Sector(code = it.sector, basePrice = it.basePrice, maxCapacity = it.maxCapacity) },
        spots = spots.map { Spot(id = it.id, sectorCode = it.sector, coordinates = Coordinates.of(it.lat, it.lng)) },
    )
}
