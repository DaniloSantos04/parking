package com.estapar.parking

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.MySQLContainer

/**
 * Sobe um MySQL real (mesma versão do docker-compose) para os testes de integração.
 * O @ServiceConnection injeta url, usuário e senha do container no datasource.
 */
@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    fun mysqlContainer(): MySQLContainer<*> = MySQLContainer("mysql:8.4")
}
