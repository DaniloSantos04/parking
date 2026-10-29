package com.estapar.parking.adapter.`in`.web.dto

import com.fasterxml.jackson.annotation.JsonFormat
import java.math.BigDecimal
import java.time.Instant

@JvmRecord
data class RevenueResponse(
    val amount: BigDecimal,
    val currency: String,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    val timestamp: Instant,
)
