package com.estapar.parking.adapter.out.persistence

import com.estapar.parking.adapter.out.persistence.repository.SectorJpaRepository
import com.estapar.parking.application.port.out.SectorPort
import com.estapar.parking.domain.model.Sector
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class SectorPersistenceAdapter(
    private val repository: SectorJpaRepository,
) : SectorPort {

    override fun findByCode(code: String): Sector? =
        repository.findById(code).map { Sector(code = it.code, basePrice = it.basePrice, maxCapacity = it.maxCapacity) }.orElse(null)

    override fun findLowestBasePrice(): BigDecimal? = repository.findLowestBasePrice()
}
