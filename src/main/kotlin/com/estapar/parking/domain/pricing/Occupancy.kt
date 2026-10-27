package com.estapar.parking.domain.pricing

/**
 * Lotação da garagem: carros dentro ([occupied]) sobre a [capacity] total.
 *
 * As comparações com percentuais usam aritmética inteira (occupied × 100 contra capacity × percentual),
 * então os limites das faixas são exatos, sem arredondamento de divisão.
 */
data class Occupancy(val occupied: Int, val capacity: Int) {

    init {
        require(occupied >= 0) { "A ocupação não pode ser negativa: $occupied" }
        require(capacity >= 0) { "A capacidade não pode ser negativa: $capacity" }
    }

    val isFull: Boolean get() = occupied >= capacity

    /** Lotação estritamente abaixo de [percent]%. */
    fun isBelow(percent: Int): Boolean = occupied.toLong() * 100 < capacity.toLong() * percent

    /** Lotação menor ou igual a [percent]%. */
    fun isAtMost(percent: Int): Boolean = occupied.toLong() * 100 <= capacity.toLong() * percent
}
