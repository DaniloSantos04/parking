package com.estapar.parking.application.service

import com.estapar.parking.GarageFixtures
import com.estapar.parking.application.port.out.FetchGarageConfigurationPort
import com.estapar.parking.application.port.out.SaveGarageConfigurationPort
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class GarageSetupServiceTest {

    private val fetch = mockk<FetchGarageConfigurationPort>()
    private val save = mockk<SaveGarageConfigurationPort>()
    private val service = GarageSetupService(fetch, save)

    @Test
    fun `busca a configuracao no simulador e grava`() {
        every { fetch.fetch() } returns GarageFixtures.CONFIGURATION
        justRun { save.save(any()) }

        val loaded = service.load()

        assertEquals(GarageFixtures.CONFIGURATION, loaded)
        verifyOrder {
            fetch.fetch()
            save.save(GarageFixtures.CONFIGURATION)
        }
    }

    @Test
    fun `nao grava nada se a busca falhar`() {
        every { fetch.fetch() } throws IllegalStateException("simulador fora do ar")

        assertThrows<IllegalStateException> { service.load() }

        verify(exactly = 0) { save.save(any()) }
    }
}
