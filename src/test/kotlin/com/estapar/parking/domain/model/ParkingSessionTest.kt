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
    fun `EXIT de carro estacionado cobra pelo preco-base do setor e o multiplicador travado`() {
        val exited = entered.park(spot, BigDecimal("10.00")).exit(entryTime.plusSeconds(2 * 3600 + 1), lowest)

        assertEquals(SessionStatus.EXITED, exited.status)
        assertFalse(exited.isActive)
        assertEquals(entryTime.plusSeconds(2 * 3600 + 1), exited.exitTime)
        assertEquals(BigDecimal("10.00"), exited.basePrice)
        assertEquals("A", exited.sectorCode)
        assertEquals(BigDecimal("33.00"), exited.amount)
    }

    @Test
    fun `EXIT sem PARKED cobra pelo preco-base informado e fica sem setor`() {
        val exited = entered.exit(entryTime.plusSeconds(3600), lowest)

        assertEquals(SessionStatus.EXITED, exited.status)
        assertNull(exited.sectorCode)
        assertNull(exited.spotId)
        assertEquals(lowest, exited.basePrice)
        assertEquals(BigDecimal("4.51"), exited.amount)
    }

    @Test
    fun `EXIT dentro dos 30 minutos gratis tem valor zero`() {
        assertEquals(BigDecimal("0.00"), entered.exit(entryTime.plusSeconds(30 * 60), lowest).amount)
    }

    @Test
    fun `EXIT anterior ao ENTRY encerra a sessao sem cobranca`() {
        val exited = entered.exit(entryTime.minusSeconds(60), lowest)

        assertEquals(SessionStatus.EXITED, exited.status)
        assertEquals(BigDecimal("0.00"), exited.amount)
    }

    @Test
    fun `nao sai duas vezes`() {
        val exited = entered.exit(entryTime.plusSeconds(60), lowest)

        assertThrows<IllegalStateException> { exited.exit(entryTime.plusSeconds(120), lowest) }
    }

    @Test
    fun `nao estaciona depois de sair`() {
        val exited = entered.exit(entryTime.plusSeconds(60), lowest)

        assertThrows<IllegalStateException> { exited.park(spot, BigDecimal("10.00")) }
    }

    private val lowest = BigDecimal("4.10")
}
