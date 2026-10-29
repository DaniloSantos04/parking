package com.estapar.parking.adapter.`in`.startup

import com.estapar.parking.application.port.`in`.LoadGarageUseCase
import com.estapar.parking.config.SimulatorProperties
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.NestedExceptionUtils
import org.springframework.stereotype.Component

@Component
class GarageStartupRunner(
    private val loadGarage: LoadGarageUseCase,
    private val properties: SimulatorProperties,
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        val maxAttempts = properties.retry.maxAttempts
        var lastFailure: Exception? = null

        for (attempt in 1..maxAttempts) {
            try {
                val configuration = loadGarage.load()
                log.info(
                    "Garagem carregada de {}: {} setores, {} vagas, capacidade {}",
                    properties.baseUrl, configuration.sectors.size, configuration.spots.size, configuration.capacity,
                )
                return
            } catch (e: Exception) {
                lastFailure = e
                log.warn("Tentativa {}/{} de carregar a garagem de {} falhou: {}", attempt, maxAttempts, properties.baseUrl, e.reason())
                if (attempt < maxAttempts) Thread.sleep(properties.retry.interval)
            }
        }

        throw GarageLoadException(
            "Não foi possível carregar a garagem de ${properties.baseUrl} após $maxAttempts tentativas: ${lastFailure?.reason()}",
            lastFailure,
        )
    }

    private fun Throwable.reason(): String {
        val description = "${javaClass.simpleName}: ${message ?: "sem mensagem"}"
        val root = NestedExceptionUtils.getMostSpecificCause(this)
        return if (root === this) description
        else "$description (causa raiz: ${root.javaClass.simpleName}: ${root.message ?: "sem mensagem"})"
    }
}

class GarageLoadException(message: String, cause: Throwable?) : RuntimeException(message, cause)
