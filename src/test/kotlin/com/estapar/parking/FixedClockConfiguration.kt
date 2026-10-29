package com.estapar.parking

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@TestConfiguration(proxyBeanMethods = false)
class FixedClockConfiguration {

    @Bean
    @Primary
    fun fixedClock(): Clock = Clock.fixed(NOW, ZoneOffset.UTC)

    companion object {
        val NOW: Instant = Instant.parse("2025-01-02T12:00:00Z")
    }
}
