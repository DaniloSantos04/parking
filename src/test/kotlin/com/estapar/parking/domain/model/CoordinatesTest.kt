package com.estapar.parking.domain.model

import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.assertEquals

class CoordinatesTest {

    @Test
    fun `normaliza latitude e longitude para 6 casas decimais`() {
        val coordinates = Coordinates.of(BigDecimal("-23.56168"), BigDecimal("-46.6559810"))

        assertEquals(BigDecimal("-23.561680"), coordinates.lat)
        assertEquals(BigDecimal("-46.655981"), coordinates.lng)
    }

    @Test
    fun `mesma posicao com escalas diferentes e igual`() {
        val fromGarage = Coordinates.of(BigDecimal("-23.561684"), BigDecimal("-46.655981"))
        val fromEvent = Coordinates.of(BigDecimal("-23.5616840"), BigDecimal("-46.65598100"))

        assertEquals(fromGarage, fromEvent)
        assertEquals(fromGarage.hashCode(), fromEvent.hashCode())
    }

    @Test
    fun `arredonda a setima casa com HALF_UP`() {
        val coordinates = Coordinates.of(BigDecimal("-23.5616845"), BigDecimal("-46.6559814"))

        assertEquals(BigDecimal("-23.561685"), coordinates.lat)
        assertEquals(BigDecimal("-46.655981"), coordinates.lng)
    }
}
