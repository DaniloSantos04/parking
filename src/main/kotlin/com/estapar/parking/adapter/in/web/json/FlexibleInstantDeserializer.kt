package com.estapar.parking.adapter.`in`.web.json

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.DateTimeParseException

class FlexibleInstantDeserializer : StdDeserializer<Instant>(Instant::class.java) {

    override fun deserialize(parser: JsonParser, context: DeserializationContext): Instant {
        val text = parser.valueAsString?.trim()
        if (text.isNullOrEmpty()) {
            return context.handleWeirdStringValue(Instant::class.java, text ?: "", "data vazia") as Instant
        }
        return try {
            when (val parsed = FORMATTER.parseBest(text, OffsetDateTime::from, LocalDateTime::from)) {
                is OffsetDateTime -> parsed.toInstant()
                else -> (parsed as LocalDateTime).toInstant(ZoneOffset.UTC)
            }
        } catch (e: DateTimeParseException) {
            context.handleWeirdStringValue(Instant::class.java, text, "esperado ISO-8601, como 2025-01-01T12:00:00.000Z") as Instant
        }
    }

    private companion object {
        val FORMATTER: DateTimeFormatter = DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            .optionalStart().appendOffsetId().optionalEnd()
            .toFormatter()
    }
}
