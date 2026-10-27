package com.estapar.parking.domain.model

import com.estapar.parking.GarageFixtures
import com.estapar.parking.GarageFixtures.SECTOR_A
import com.estapar.parking.GarageFixtures.SECTOR_B
import com.estapar.parking.GarageFixtures.spot
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import kotlin.test.assertContains
import kotlin.test.assertEquals

class GarageConfigurationTest {

    @Test
    fun `capacidade da garagem e a soma das capacidades dos setores`() {
        assertEquals(5, GarageFixtures.CONFIGURATION.capacity)
    }

    @Test
    fun `exige ao menos um setor`() {
        val error = assertThrows<IllegalArgumentException> { GarageConfiguration(emptyList(), emptyList()) }

        assertContains(error.message!!, "ao menos um setor")
    }

    @Test
    fun `rejeita setores duplicados`() {
        val error = assertThrows<IllegalArgumentException> {
            GarageConfiguration(listOf(SECTOR_A, SECTOR_A.copy(maxCapacity = 9)), emptyList())
        }

        assertContains(error.message!!, "Setores duplicados")
    }

    @Test
    fun `rejeita vagas com id duplicado`() {
        val error = assertThrows<IllegalArgumentException> {
            GarageConfiguration(
                listOf(SECTOR_A),
                listOf(spot(1, "A", "-23.1", "-46.1"), spot(1, "A", "-23.2", "-46.2")),
            )
        }

        assertContains(error.message!!, "id duplicado")
    }

    @Test
    fun `rejeita vagas com as mesmas coordenadas`() {
        val error = assertThrows<IllegalArgumentException> {
            GarageConfiguration(
                listOf(SECTOR_A),
                listOf(spot(1, "A", "-23.1", "-46.1"), spot(2, "A", "-23.100000", "-46.100000")),
            )
        }

        assertContains(error.message!!, "coordenadas duplicadas")
    }

    @Test
    fun `rejeita vaga de setor inexistente`() {
        val error = assertThrows<IllegalArgumentException> {
            GarageConfiguration(listOf(SECTOR_A, SECTOR_B), listOf(spot(7, "Z", "-23.1", "-46.1")))
        }

        assertContains(error.message!!, "setores inexistentes: [7]")
    }

    @Test
    fun `rejeita setor com preco negativo`() {
        assertThrows<IllegalArgumentException> { Sector(code = "A", basePrice = BigDecimal("-0.01"), maxCapacity = 1) }
    }

    @Test
    fun `rejeita setor com capacidade negativa`() {
        assertThrows<IllegalArgumentException> { Sector(code = "A", basePrice = BigDecimal.TEN, maxCapacity = -1) }
    }
}
