package com.estapar.parking.config

import com.estapar.parking.domain.pricing.OccupancyPricingPolicy
import com.estapar.parking.domain.pricing.PricingPolicy
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

/** Registra como beans as peças do domínio, que não conhecem o Spring. */
@Configuration(proxyBeanMethods = false)
class DomainConfig {

    @Bean
    fun pricingPolicy(): PricingPolicy = OccupancyPricingPolicy()

    /** Relógio em UTC, injetado para que o "agora" seja controlável nos testes. */
    @Bean
    fun clock(): Clock = Clock.systemUTC()
}
