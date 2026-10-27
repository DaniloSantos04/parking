package com.estapar.parking.adapter.`in`.web.dto

import com.estapar.parking.adapter.`in`.web.json.FlexibleInstantDeserializer
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

/** Corpo do POST /webhook. O tipo concreto é escolhido pelo campo `event_type`. */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "event_type")
@JsonSubTypes(
    JsonSubTypes.Type(value = EntryEvent::class, name = "ENTRY"),
)
sealed interface WebhookEvent {
    val licensePlate: String
}

@JvmRecord
data class EntryEvent(
    @field:NotBlank @field:Size(max = MAX_PLATE_LENGTH)
    @JsonProperty("license_plate")
    override val licensePlate: String,

    @JsonProperty("entry_time")
    @JsonDeserialize(using = FlexibleInstantDeserializer::class)
    val entryTime: Instant,
) : WebhookEvent

/** Limite da coluna license_plate no banco. */
const val MAX_PLATE_LENGTH = 20
