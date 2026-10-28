package com.estapar.parking.domain.model

object LicensePlate {
    /**
     * Tamanho máximo da placa. Folga sobre os 7 caracteres da placa brasileira, porque o simulador
     * envia formatos de 6 a 8 caracteres. As colunas license_plate das migrations usam o mesmo valor.
     */
    const val MAX_LENGTH = 20

    /** Forma canônica da placa: sem espaços nas pontas e em maiúsculas ("zul0001 " e "ZUL0001" são o mesmo carro). */
    fun normalize(raw: String): String = raw.trim().uppercase()
}
