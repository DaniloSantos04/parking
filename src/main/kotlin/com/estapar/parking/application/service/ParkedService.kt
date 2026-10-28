package com.estapar.parking.application.service

import com.estapar.parking.application.port.`in`.ParkedCommand
import com.estapar.parking.application.port.`in`.RegisterParkedUseCase
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
class ParkedService(
    private val lockGarage: LockGaragePort,
    private val sessions: SessionPort,
    private val spots: SpotPort,
    private val sectors: SectorPort,
    private val anomalies: AnomalyRecorder,
) : RegisterParkedUseCase {

    private val log = LoggerFactory.getLogger(javaClass)

    /** Sob o lock da garagem, para dois PARKED simultâneos não ocuparem a mesma vaga. */
    @Transactional
    override fun registerParked(command: ParkedCommand) {
        lockGarage.lockAndGetCapacity()
        val plate = command.licensePlate
        val position = "(${command.coordinates.lat}, ${command.coordinates.lng})"

        val session = sessions.findActive(plate)
        if (session == null) {
            anomalies.record(AnomalyType.PARKED_WITHOUT_ENTRY, plate, "PARKED em $position ignorado: placa sem sessão ativa")
            return
        }
        if (session.status == SessionStatus.PARKED) {
            anomalies.record(
                AnomalyType.ALREADY_PARKED, plate,
                "PARKED em $position ignorado: a placa já está estacionada na vaga ${session.spotId}",
            )
            return
        }

        val spot = spots.findByCoordinates(command.coordinates)
        if (spot == null) {
            anomalies.record(AnomalyType.SPOT_NOT_FOUND, plate, "PARKED em $position ignorado: nenhuma vaga nessas coordenadas")
            return
        }
        if (spots.isOccupied(spot.id)) {
            anomalies.record(
                AnomalyType.SPOT_OCCUPIED, plate,
                "PARKED na vaga ${spot.id} $position ignorado: a vaga já está ocupada; a sessão continua ENTERED",
            )
            return
        }

        val sector = sectors.findByCode(spot.sectorCode)
            ?: throw IllegalStateException("Vaga ${spot.id} aponta para o setor inexistente ${spot.sectorCode}")
        sessions.save(session.park(spot, sector.basePrice))
        spots.markOccupied(spot.id)
        log.info("PARKED {} na vaga {} do setor {} (preço-base {})", plate, spot.id, sector.code, sector.basePrice)
    }
}
