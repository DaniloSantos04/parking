package com.estapar.parking.adapter.`in`.web.json

import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.Instant
import kotlin.test.assertEquals

class FlexibleInstantDeserializerTest {

    private val mapper = JsonMapper.builder()
        .addModule(SimpleModule().addDeserializer(Instant::class.java, FlexibleInstantDeserializer()))
        .build()

    private fun read(text: String): Instant = mapper.readValue("\"$text\"", Instant::class.java)

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource(
        "2025-01-01T12:00:00.000Z, 2025-01-01T12:00:00Z",
        "2026-10-27T22:00:24, 2026-10-27T22:00:24Z",
        "2025-01-01T12:00:00.123, 2025-01-01T12:00:00.123Z",
        "2025-01-01T09:00:00-03:00, 2025-01-01T12:00:00Z",
        "2025-01-01T12:00:00+00:00, 2025-01-01T12:00:00Z",
    )
    fun `aceita datas com e sem fuso`(text: String, expected: Instant) {
        assertEquals(expected, read(text))
    }

    @Test
    fun `rejeita texto que nao e data`() {
        assertThrows<InvalidFormatException> { read("ontem") }
    }

    @Test
    fun `rejeita data vazia`() {
        assertThrows<InvalidFormatException> { read("") }
    }
}
