package com.estapar.parking.application.service

import com.estapar.parking.GarageFixtures
import com.estapar.parking.application.port.`in`.RevenueQuery
import com.estapar.parking.application.port.`in`.SectorNotFoundException
import com.estapar.parking.application.port.out.RevenuePort
import com.estapar.parking.application.port.out.SectorPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.assertEquals

class RevenueServiceTest {

    private val revenuePort = mockk<RevenuePort>()
    private val sectors = mockk<SectorPort>()

    private val now = Instant.parse("2025-01-02T12:00:00Z")
    private val service = RevenueService(revenuePort, sectors, Clock.fixed(now, ZoneOffset.UTC))

    @BeforeEach
    fun setUp() {
        every { revenuePort.sumExitedAmount(any(), any(), any()) } returns BigDecimal("10.50")
        every { sectors.findByCode("A") } returns GarageFixtures.SECTOR_A
        every { sectors.findByCode("Z") } returns null
    }

    @Test
    fun `dia passado soma da meia-noite a meia-noite de Sao Paulo`() {
        service.getRevenue(RevenueQuery(LocalDate.parse("2025-01-01"), null))

        verify {
            revenuePort.sumExitedAmount(Instant.parse("2025-01-01T03:00:00Z"), Instant.parse("2025-01-02T03:00:00Z"), null)
        }
    }

    @Test
    fun `sem data usa o dia atual em Sao Paulo e soma ate o momento`() {
        service.getRevenue(RevenueQuery(null, null))

        verify { revenuePort.sumExitedAmount(Instant.parse("2025-01-02T03:00:00Z"), now, null) }
    }

    @Test
    fun `o dia atual e o de Sao Paulo, nao o de UTC`() {
        val lateEvening = Instant.parse("2025-01-02T01:00:00Z")
        val service = RevenueService(revenuePort, sectors, Clock.fixed(lateEvening, ZoneOffset.UTC))

        service.getRevenue(RevenueQuery(null, null))

        verify { revenuePort.sumExitedAmount(Instant.parse("2025-01-01T03:00:00Z"), lateEvening, null) }
    }

    @Test
    fun `dia futuro tem receita zero sem consultar o banco`() {
        val result = service.getRevenue(RevenueQuery(LocalDate.parse("2025-01-03"), null))

        assertEquals(BigDecimal("0.00"), result.amount)
        verify(exactly = 0) { revenuePort.sumExitedAmount(any(), any(), any()) }
    }

    @Test
    fun `filtra pelo setor informado`() {
        service.getRevenue(RevenueQuery(LocalDate.parse("2025-01-01"), "A"))

        verify { revenuePort.sumExitedAmount(any(), any(), "A") }
    }

    @Test
    fun `setor inexistente lanca SectorNotFoundException sem consultar a receita`() {
        val error = assertThrows<SectorNotFoundException> { service.getRevenue(RevenueQuery(null, "Z")) }

        assertEquals("Z", error.sectorCode)
        verify(exactly = 0) { revenuePort.sumExitedAmount(any(), any(), any()) }
    }

    @Test
    fun `responde em BRL, com 2 casas e o instante da apuracao`() {
        every { revenuePort.sumExitedAmount(any(), any(), any()) } returns BigDecimal("5")

        val result = service.getRevenue(RevenueQuery(null, null))

        assertEquals(BigDecimal("5.00"), result.amount)
        assertEquals("BRL", result.currency)
        assertEquals(now, result.timestamp)
    }
}
