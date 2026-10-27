package com.estapar.parking.domain.model

object LicensePlate {
    /** Forma canônica da placa: sem espaços nas pontas e em maiúsculas ("zul0001 " e "ZUL0001" são o mesmo carro). */
    fun normalize(raw: String): String = raw.trim().uppercase()
}
