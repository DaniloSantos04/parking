package com.estapar.parking.application.service

import com.estapar.parking.application.port.`in`.ExitCommand
import com.estapar.parking.application.port.`in`.RegisterExitUseCase
import com.estapar.parking.application.port.out.LockGaragePort
import com.estapar.parking.application.port.out.SectorPort
import com.estapar.parking.application.port.out.SessionPort
import com.estapar.parking.application.port.out.SpotPort
import com.estapar.parking.domain.model.AnomalyType
import com.estapar.parking.domain.model.SessionStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ExitService(
    private val lockGarage: LockGaragePort,
    private val sessions: SessionPort,
    private val spots: SpotPort,
    private val sectors: SectorPort,
    private val anomalies: AnomalyRecorder,
) : RegisterExitUseCase {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun registerExit(command: ExitCommand) {
        lockGarage.lockAndGetCapacity()
        val plate = command.licensePlate

        val session = sessions.findActive(plate)
        if (session == null) {
            anomalies.record(
                AnomalyType.EXIT_WITHOUT_ACTIVE_SESSION, plate,
                "EXIT às ${command.exitTime} ignorado: placa sem sessão ativa (nunca entrou ou já saiu)",
            )
            return
        }

        val lowestBasePrice = sectors.findLowestBasePrice()
            ?: throw IllegalStateException("Nenhum setor cadastrado; a garagem não foi carregada")
        val exited = sessions.save(session.exit(command.exitTime, lowestBasePrice))
        session.spotId?.let { spots.release(it) }

        if (session.status == SessionStatus.ENTERED) {
            anomalies.record(
                AnomalyType.EXIT_WITHOUT_PARKED, plate,
                "EXIT às ${command.exitTime} de carro sem PARKED: cobrado R$ ${exited.amount} pelo menor preço-base (${exited.basePrice}), sem setor",
            )
        }
        if (command.exitTime.isBefore(session.entryTime)) {
            anomalies.record(
                AnomalyType.EXIT_BEFORE_ENTRY, plate,
                "EXIT às ${command.exitTime} anterior ao ENTRY às ${session.entryTime}: sessão encerrada sem cobrança",
            )
        }
        log.info(
            "EXIT {} às {}: entrada {}, setor {}, preço-base {}, multiplicador {}, valor R$ {}",
            plate, command.exitTime, session.entryTime, exited.sectorCode, exited.basePrice, exited.priceMultiplier, exited.amount,
        )
    }
}
