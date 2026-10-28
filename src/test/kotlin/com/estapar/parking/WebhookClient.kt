package com.estapar.parking

import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.post

/** Envia eventos ao POST /webhook nos testes de integração, no formato do enunciado. */
class WebhookClient(private val mockMvc: MockMvc) {

    fun post(body: String): ResultActionsDsl = mockMvc.post("/webhook") {
        contentType = MediaType.APPLICATION_JSON
        content = body
    }

    fun entry(plate: String, time: String = "2025-01-01T12:00:00.000Z") =
        post("""{"license_plate": "$plate", "entry_time": "$time", "event_type": "ENTRY"}""")

    fun parked(plate: String, lat: String, lng: String) =
        post("""{"license_plate": "$plate", "lat": $lat, "lng": $lng, "event_type": "PARKED"}""")
}
