package com.estapar.parking

import com.estapar.parking.adapter.out.persistence.entity.GarageEntity
import com.estapar.parking.adapter.out.persistence.repository.GarageJpaRepository
import com.estapar.parking.adapter.out.persistence.repository.SectorJpaRepository
import com.estapar.parking.adapter.out.persistence.repository.SpotJpaRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals

@IntegrationTest
class ParkingApplicationTests(
    @Autowired private val garageRepository: GarageJpaRepository,
    @Autowired private val sectorRepository: SectorJpaRepository,
    @Autowired private val spotRepository: SpotJpaRepository,
) {

    @Test
    fun `carrega a configuracao da garagem na subida da aplicacao`() {
        assertEquals(setOf("A", "B"), sectorRepository.findAll().map { it.code }.toSet())
        assertEquals(5, spotRepository.count())
        assertEquals(5, garageRepository.findById(GarageEntity.SINGLETON_ID).orElseThrow().capacity)
    }
}
