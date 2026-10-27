package com.estapar.parking.application.service

import com.estapar.parking.application.port.out.AnomalyPort
import com.estapar.parking.domain.model.Anomaly
import com.estapar.parking.domain.model.AnomalyType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Clock

/** Registra uma anomalia no log e no banco, com o horário do relógio da aplicação. */
@Component
class AnomalyRecorder(
    private val anomalyPort: AnomalyPort,
    private val clock: Clock,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun record(type: AnomalyType, licensePlate: String?, detail: String) {
        log.warn("Anomalia {} (placa {}): {}", type, licensePlate, detail)
        anomalyPort.save(Anomaly(type = type, licensePlate = licensePlate, detail = detail, occurredAt = clock.instant()))
    }
}
