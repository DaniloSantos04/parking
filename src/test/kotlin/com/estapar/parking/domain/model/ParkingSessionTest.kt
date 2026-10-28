package com.estapar.parking.domain.model

import com.estapar.parking.GarageFixtures
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ParkingSessionTest {

    private val entryTime = Instant.parse("2025-01-01T12:00:00Z")
    private val entered = ParkingSession.enter("ZUL0001", entryTime, BigDecimal("1.10"))
    private val spot = GarageFixtures.SPOTS.first()

    @Test
    fun `ENTRY cria sessao ativa, sem vaga nem setor`() {
        assertEquals(SessionStatus.ENTERED, entered.status)
        assertTrue(entered.isActive)
        assertNull(entered.spotId)
        assertNull(entered.sectorCode)
        assertNull(entered.basePrice)
    }

    @Test
    fun `PARKED associa vaga, setor e preco-base e mantem entrada e multiplicador`() {
        val parked = entered.park(spot, BigDecimal("10.00"))

        assertEquals(SessionStatus.PARKED, parked.status)
        assertEquals(spot.id, parked.spotId)
        assertEquals("A", parked.sectorCode)
        assertEquals(BigDecimal("10.00"), parked.basePrice)
        assertEquals(entryTime, parked.entryTime)
        assertEquals(BigDecimal("1.10"), parked.priceMultiplier)
        assertTrue(parked.isActive)
    }

    @Test
    fun `nao estaciona duas vezes`() {
        val parked = entered.park(spot, BigDecimal("10.00"))

        assertThrows<IllegalStateException> { parked.park(spot, BigDecimal("10.00")) }
    }

    @Test
    fun `sessao encerrada nao esta ativa`() {
        assertFalse(entered.copy(status = SessionStatus.EXITED).isActive)
    }
}
