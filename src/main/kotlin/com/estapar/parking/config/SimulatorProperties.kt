package com.estapar.parking.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/** Endereço do simulador e política de novas tentativas da carga inicial da garagem. */
@JvmRecord
@ConfigurationProperties("estapar.simulator")
data class SimulatorProperties(
    val baseUrl: String,
    val retry: Retry,
) {
    @JvmRecord
    data class Retry(
        val maxAttempts: Int,
        val interval: Duration,
    ) {
        init {
            require(maxAttempts >= 1) { "estapar.simulator.retry.max-attempts deve ser ao menos 1" }
            require(!interval.isNegative) { "estapar.simulator.retry.interval não pode ser negativo" }
        }
    }
}
