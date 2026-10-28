package com.estapar.parking

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/** Relógio fixo nos testes de integração, para "hoje" e "até o momento" serem determinísticos. */
@TestConfiguration(proxyBeanMethods = false)
class FixedClockConfiguration {

    @Bean
    @Primary
    fun fixedClock(): Clock = Clock.fixed(NOW, ZoneOffset.UTC)

    companion object {
        /** 2 de janeiro de 2025, 09:00 em São Paulo. */
        val NOW: Instant = Instant.parse("2025-01-02T12:00:00Z")
    }
}
