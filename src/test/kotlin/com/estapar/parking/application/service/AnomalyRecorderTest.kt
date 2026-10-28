package com.estapar.parking.application.service

import com.estapar.parking.application.port.out.AnomalyPort
import com.estapar.parking.domain.model.Anomaly
import com.estapar.parking.domain.model.AnomalyType
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class AnomalyRecorderTest {

    private val now = Instant.parse("2025-01-01T12:34:56Z")
    private val port = mockk<AnomalyPort>()
    private val recorder = AnomalyRecorder(port, Clock.fixed(now, ZoneOffset.UTC))

    @Test
    fun `grava a anomalia com o horario do relogio da aplicacao`() {
        justRun { port.save(any()) }

        recorder.record(AnomalyType.GARAGE_FULL, "ZUL0001", "garagem cheia")

        verify { port.save(Anomaly(AnomalyType.GARAGE_FULL, "ZUL0001", "garagem cheia", now)) }
    }
}
