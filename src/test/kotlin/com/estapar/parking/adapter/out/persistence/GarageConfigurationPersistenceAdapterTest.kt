package com.estapar.parking.adapter.out.persistence

import com.estapar.parking.GarageFixtures
import com.estapar.parking.GarageFixtures.SECTOR_A
import com.estapar.parking.GarageFixtures.SECTOR_B
import com.estapar.parking.GarageFixtures.spot
import com.estapar.parking.IntegrationTest
import com.estapar.parking.adapter.out.persistence.entity.GarageEntity
import com.estapar.parking.adapter.out.persistence.repository.GarageJpaRepository
import com.estapar.parking.adapter.out.persistence.repository.SectorJpaRepository
import com.estapar.parking.adapter.out.persistence.repository.SpotJpaRepository
import com.estapar.parking.domain.model.GarageConfiguration
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@IntegrationTest
class GarageConfigurationPersistenceAdapterTest(
    @Autowired private val adapter: GarageConfigurationPersistenceAdapter,
    @Autowired private val garageRepository: GarageJpaRepository,
    @Autowired private val sectorRepository: SectorJpaRepository,
    @Autowired private val spotRepository: SpotJpaRepository,
) {

    /** Volta ao estado deixado pela carga da subida, para não afetar os outros testes. */
    @AfterEach
    fun restoreStartupState() {
        spotRepository.saveAll(spotRepository.findAll().onEach { it.occupied = false })
        adapter.save(GarageFixtures.CONFIGURATION)
    }

    @Test
    fun `gravar a mesma configuracao de novo nao duplica nada`() {
        adapter.save(GarageFixtures.CONFIGURATION)
        adapter.save(GarageFixtures.CONFIGURATION)

        assertEquals(2, sectorRepository.count())
        assertEquals(5, spotRepository.count())
        assertEquals(1, garageRepository.count())
        assertEquals(5, capacity())
    }

    @Test
    fun `grava precos e coordenadas com a escala do banco`() {
        adapter.save(GarageFixtures.CONFIGURATION)

        assertEquals(BigDecimal("4.10"), sectorRepository.findById("B").orElseThrow().basePrice)
        val spot = spotRepository.findById(1).orElseThrow()
        assertEquals(BigDecimal("-23.561684"), spot.lat)
        assertEquals(BigDecimal("-46.655981"), spot.lng)
        assertEquals("A", spot.sectorCode)
    }

    @Test
    fun `recarga atualiza setores, vagas e capacidade`() {
        val changed = GarageConfiguration(
            sectors = listOf(SECTOR_A.copy(basePrice = BigDecimal("12.50"), maxCapacity = 4), SECTOR_B),
            spots = GarageFixtures.SPOTS + spot(6, "A", "-23.561584", "-46.655881"),
        )

        adapter.save(changed)

        assertEquals(BigDecimal("12.50"), sectorRepository.findById("A").orElseThrow().basePrice)
        assertEquals(6, spotRepository.count())
        assertEquals(7, capacity())
        spotRepository.deleteById(6)
    }

    @Test
    fun `recarga preserva a ocupacao atual das vagas`() {
        val spot = spotRepository.findById(3).orElseThrow()
        spot.occupied = true
        spotRepository.save(spot)

        adapter.save(GarageFixtures.CONFIGURATION)

        assertTrue(spotRepository.findById(3).orElseThrow().occupied)
    }

    private fun capacity() = garageRepository.findById(GarageEntity.SINGLETON_ID).orElseThrow().capacity
}
