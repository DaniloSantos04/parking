package com.estapar.parking

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@SpringBootTest
@AutoConfigureMockMvc
@Import(
    TestcontainersConfiguration::class,
    FakeSimulatorConfiguration::class,
    FixedClockConfiguration::class,
    DatabaseCleaner::class,
)
annotation class IntegrationTest
