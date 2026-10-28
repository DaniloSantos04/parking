package com.estapar.parking.adapter.out.persistence

import com.estapar.parking.adapter.out.persistence.repository.ParkingSessionJpaRepository
import com.estapar.parking.application.port.out.RevenuePort
import com.estapar.parking.domain.model.SessionStatus
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.Instant

@Component
class RevenuePersistenceAdapter(
    private val repository: ParkingSessionJpaRepository,
) : RevenuePort {

    override fun sumExitedAmount(from: Instant, to: Instant, sectorCode: String?): BigDecimal =
        repository.sumAmount(SessionStatus.EXITED, from, to, sectorCode)
}
