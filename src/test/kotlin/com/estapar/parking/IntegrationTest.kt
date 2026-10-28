package com.estapar.parking

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import

/**
 * Teste de integração com a aplicação completa, MySQL real (Testcontainers) e simulador falso.
 * Todas as classes anotadas compartilham o mesmo contexto Spring e o mesmo container.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class, FakeSimulatorConfiguration::class)
annotation class IntegrationTest
