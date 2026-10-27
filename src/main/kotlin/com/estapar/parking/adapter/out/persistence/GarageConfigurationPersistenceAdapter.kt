package com.estapar.parking.adapter.out.persistence

import com.estapar.parking.adapter.out.persistence.entity.GarageEntity
import com.estapar.parking.adapter.out.persistence.entity.SectorEntity
import com.estapar.parking.adapter.out.persistence.entity.SpotEntity
import com.estapar.parking.adapter.out.persistence.repository.GarageJpaRepository
import com.estapar.parking.adapter.out.persistence.repository.SectorJpaRepository
import com.estapar.parking.adapter.out.persistence.repository.SpotJpaRepository
import com.estapar.parking.application.port.out.SaveGarageConfigurationPort
import com.estapar.parking.domain.model.GarageConfiguration
import com.estapar.parking.domain.model.Sector
import com.estapar.parking.domain.model.Spot
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class GarageConfigurationPersistenceAdapter(
    private val garageRepository: GarageJpaRepository,
    private val sectorRepository: SectorJpaRepository,
    private val spotRepository: SpotJpaRepository,
) : SaveGarageConfigurationPort {

    /**
     * Upsert por chave: setores pelo código, vagas pelo id. Uma vaga que já existe mantém o
     * campo occupied, que pertence ao estado da aplicação e não à configuração do simulador.
     */
    @Transactional
    override fun save(configuration: GarageConfiguration) {
        upsertSectors(configuration.sectors)
        upsertSpots(configuration.spots)
        garageRepository.save(GarageEntity(capacity = configuration.capacity))
    }

    private fun upsertSectors(sectors: List<Sector>) {
        val existing = sectorRepository.findAllById(sectors.map { it.code }).associateBy { it.code }
        val entities = sectors.map { sector ->
            existing[sector.code]
                ?.apply {
                    basePrice = sector.basePrice
                    maxCapacity = sector.maxCapacity
                }
                ?: SectorEntity(code = sector.code, basePrice = sector.basePrice, maxCapacity = sector.maxCapacity)
        }
        sectorRepository.saveAll(entities)
    }

    private fun upsertSpots(spots: List<Spot>) {
        val existing = spotRepository.findAllById(spots.map { it.id }).associateBy { it.id }
        val entities = spots.map { spot ->
            existing[spot.id]
                ?.apply {
                    sectorCode = spot.sectorCode
                    lat = spot.coordinates.lat
                    lng = spot.coordinates.lng
                }
                ?: SpotEntity(
                    id = spot.id,
                    sectorCode = spot.sectorCode,
                    lat = spot.coordinates.lat,
                    lng = spot.coordinates.lng,
                )
        }
        spotRepository.saveAll(entities)
    }
}
