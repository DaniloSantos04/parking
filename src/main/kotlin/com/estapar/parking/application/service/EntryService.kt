package com.estapar.parking.application.service

import com.estapar.parking.application.port.`in`.EntryCommand
import com.estapar.parking.application.port.`in`.RegisterEntryUseCase
import com.estapar.parking.application.port.out.LockGaragePort
import com.estapar.parking.application.port.out.SessionPort
import com.estapar.parking.domain.model.AnomalyType
import com.estapar.parking.domain.model.ParkingSession
import com.estapar.parking.domain.pricing.EntryPricing
import com.estapar.parking.domain.pricing.Occupancy
import com.estapar.parking.domain.pricing.PricingPolicy
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class EntryService(
    private val lockGarage: LockGaragePort,
    private val sessions: SessionPort,
    private val pricingPolicy: PricingPolicy,
    private val anomalies: AnomalyRecorder,
) : RegisterEntryUseCase {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * O lock da garagem vem primeiro: a contagem da lotação e a criação da sessão acontecem sem
     * que outra entrada concorrente veja a mesma vaga livre.
     */
    @Transactional
    override fun registerEntry(command: EntryCommand) {
        val capacity = lockGarage.lockAndGetCapacity()

        if (sessions.findActive(command.licensePlate) != null) {
            anomalies.record(
                AnomalyType.DUPLICATE_ENTRY,
                command.licensePlate,
                "ENTRY às ${command.entryTime} ignorado: a placa já tem sessão ativa",
            )
            return
        }

        val occupancy = Occupancy(occupied = sessions.countActive(), capacity = capacity)
        when (val pricing = pricingPolicy.priceEntry(occupancy)) {
            EntryPricing.GarageFull -> anomalies.record(
                AnomalyType.GARAGE_FULL,
                command.licensePlate,
                "ENTRY às ${command.entryTime} recusado: garagem cheia (${occupancy.occupied}/${occupancy.capacity})",
            )

            is EntryPricing.Allowed -> {
                sessions.save(ParkingSession.enter(command.licensePlate, command.entryTime, pricing.multiplier))
                log.info(
                    "ENTRY {} às {}: lotação antes da entrada {}/{}, multiplicador {}",
                    command.licensePlate, command.entryTime, occupancy.occupied, occupancy.capacity, pricing.multiplier,
                )
            }
        }
    }
}
