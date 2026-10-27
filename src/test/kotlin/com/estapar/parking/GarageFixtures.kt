package com.estapar.parking

import com.estapar.parking.domain.model.Coordinates
import com.estapar.parking.domain.model.GarageConfiguration
import com.estapar.parking.domain.model.Sector
import com.estapar.parking.domain.model.Spot
import java.math.BigDecimal

/** Garagem pequena usada nos testes: setor A com 2 vagas e setor B com 3, capacidade 5. */
object GarageFixtures {

    val SECTOR_A = Sector(code = "A", basePrice = BigDecimal("10.00"), maxCapacity = 2)
    val SECTOR_B = Sector(code = "B", basePrice = BigDecimal("4.10"), maxCapacity = 3)

    val SPOTS = listOf(
        spot(1, "A", "-23.561684", "-46.655981"),
        spot(2, "A", "-23.561664", "-46.655961"),
        spot(3, "B", "-23.561644", "-46.655941"),
        spot(4, "B", "-23.561624", "-46.655921"),
        spot(5, "B", "-23.561604", "-46.655901"),
    )

    val CONFIGURATION = GarageConfiguration(sectors = listOf(SECTOR_A, SECTOR_B), spots = SPOTS)

    fun spot(id: Long, sector: String, lat: String, lng: String) =
        Spot(id = id, sectorCode = sector, coordinates = Coordinates.of(BigDecimal(lat), BigDecimal(lng)))
}
