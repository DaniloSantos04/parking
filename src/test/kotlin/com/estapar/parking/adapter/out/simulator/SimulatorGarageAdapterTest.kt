package com.estapar.parking.adapter.out.simulator

import com.estapar.parking.GarageFixtures.spot
import com.estapar.parking.config.SimulatorProperties
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.HttpServerErrorException
import java.math.BigDecimal
import kotlin.test.assertEquals

@RestClientTest(SimulatorGarageAdapter::class)
@EnableConfigurationProperties(SimulatorProperties::class)
class SimulatorGarageAdapterTest(
    @Autowired private val adapter: SimulatorGarageAdapter,
    @Autowired private val server: MockRestServiceServer,
) {

    @AfterEach
    fun verifyServer() = server.verify()

    @Test
    fun `le o formato real do simulador, com base_price e campos extras`() {
        server.expect(requestTo("http://localhost:3000/garage"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(ClassPathResource("simulator/garage.json"), MediaType.APPLICATION_JSON))

        val configuration = adapter.fetch()

        assertEquals(listOf("A", "B"), configuration.sectors.map { it.code })
        assertEquals(BigDecimal("40.5"), configuration.sectors[0].basePrice)
        assertEquals(10, configuration.sectors[0].maxCapacity)
        assertEquals(BigDecimal("4.1"), configuration.sectors[1].basePrice)
        assertEquals(30, configuration.capacity)
        assertEquals(
            listOf(
                spot(1, "A", "-23.561684", "-46.655981"),
                spot(2, "A", "-23.561664", "-46.655961"),
                spot(11, "B", "-23.561484", "-46.655781"),
            ),
            configuration.spots,
        )
    }

    @Test
    fun `le o formato do exemplo do enunciado, com basePrice`() {
        val body = """
            {
              "garage": [{ "sector": "A", "basePrice": 10.0, "max_capacity": 100 }],
              "spots": [{ "id": 1, "sector": "A", "lat": -23.561684, "lng": -46.655981 }]
            }
        """.trimIndent()
        server.expect(requestTo("http://localhost:3000/garage"))
            .andRespond(withSuccess(body, MediaType.APPLICATION_JSON))

        val configuration = adapter.fetch()

        assertEquals(BigDecimal("10.0"), configuration.sectors.single().basePrice)
        assertEquals(100, configuration.capacity)
    }

    @Test
    fun `propaga erro HTTP do simulador`() {
        server.expect(requestTo("http://localhost:3000/garage"))
            .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE))

        assertThrows<HttpServerErrorException> { adapter.fetch() }
    }

    @Test
    fun `rejeita configuracao inconsistente`() {
        val body = """
            {
              "garage": [{ "sector": "A", "base_price": 10.0, "max_capacity": 1 }],
              "spots": [{ "id": 1, "sector": "Z", "lat": -23.561684, "lng": -46.655981 }]
            }
        """.trimIndent()
        server.expect(requestTo("http://localhost:3000/garage"))
            .andRespond(withSuccess(body, MediaType.APPLICATION_JSON))

        assertThrows<IllegalArgumentException> { adapter.fetch() }
    }
}
