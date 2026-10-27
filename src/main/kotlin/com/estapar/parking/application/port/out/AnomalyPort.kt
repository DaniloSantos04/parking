package com.estapar.parking.application.port.out

import com.estapar.parking.domain.model.Anomaly

interface AnomalyPort {
    fun save(anomaly: Anomaly)
}
