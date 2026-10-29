package com.estapar.parking.config

import com.estapar.parking.domain.pricing.OccupancyPricingPolicy
import com.estapar.parking.domain.pricing.PricingPolicy
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration(proxyBeanMethods = false)
class DomainConfig {

    @Bean
    fun pricingPolicy(): PricingPolicy = OccupancyPricingPolicy()

    @Bean
    fun clock(): Clock = Clock.systemUTC()
}
