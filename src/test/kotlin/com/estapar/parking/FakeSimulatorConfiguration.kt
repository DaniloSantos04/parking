package com.estapar.parking

import com.estapar.parking.application.port.out.FetchGarageConfigurationPort
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

/**
 * Substitui o simulador nos testes de integração: a carga da garagem na subida usa
 * [GarageFixtures.CONFIGURATION] em vez de chamar o GET /garage real.
 */
@TestConfiguration(proxyBeanMethods = false)
class FakeSimulatorConfiguration {

    @Bean
    @Primary
    fun fakeGarageConfigurationSource(): FetchGarageConfigurationPort =
        object : FetchGarageConfigurationPort {
            override fun fetch() = GarageFixtures.CONFIGURATION
        }
}
