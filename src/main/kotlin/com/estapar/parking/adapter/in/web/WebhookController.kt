package com.estapar.parking.adapter.`in`.web

import com.estapar.parking.adapter.`in`.web.dto.EntryEvent
import com.estapar.parking.adapter.`in`.web.dto.ExitEvent
import com.estapar.parking.adapter.`in`.web.dto.ParkedEvent
import com.estapar.parking.adapter.`in`.web.dto.WebhookEvent
import com.estapar.parking.application.port.`in`.EntryCommand
import com.estapar.parking.application.port.`in`.ExitCommand
import com.estapar.parking.application.port.`in`.ParkedCommand
import com.estapar.parking.application.port.`in`.RegisterEntryUseCase
import com.estapar.parking.application.port.`in`.RegisterExitUseCase
import com.estapar.parking.application.port.`in`.RegisterParkedUseCase
import com.estapar.parking.domain.model.Coordinates
import com.estapar.parking.domain.model.LicensePlate
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

/**
 * Recebe os eventos do simulador. Responde 200 sempre que o evento é bem formado, inclusive
 * quando a regra de negócio o recusa (a recusa vira anomalia registrada). Payload malformado
 * responde 400.
 */
@RestController
class WebhookController(
    private val registerEntry: RegisterEntryUseCase,
    private val registerParked: RegisterParkedUseCase,
    private val registerExit: RegisterExitUseCase,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping("/webhook")
    fun receive(@Valid @RequestBody event: WebhookEvent): ResponseEntity<Void> {
        log.info("Evento recebido no webhook: {}", event)
        val plate = LicensePlate.normalize(event.licensePlate)
        when (event) {
            is EntryEvent -> registerEntry.registerEntry(EntryCommand(licensePlate = plate, entryTime = event.entryTime))
            is ParkedEvent -> registerParked.registerParked(
                ParkedCommand(licensePlate = plate, coordinates = Coordinates.of(event.lat, event.lng)),
            )
            is ExitEvent -> registerExit.registerExit(ExitCommand(licensePlate = plate, exitTime = event.exitTime))
        }
        return ResponseEntity.ok().build()
    }
}
