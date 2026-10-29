package com.estapar.parking.domain.pricing

data class Occupancy(val occupied: Int, val capacity: Int) {

    init {
        require(occupied >= 0) { "A ocupação não pode ser negativa: $occupied" }
        require(capacity >= 0) { "A capacidade não pode ser negativa: $capacity" }
    }

    val isFull: Boolean get() = occupied >= capacity

    fun isBelow(percent: Int): Boolean = occupied.toLong() * 100 < capacity.toLong() * percent

    fun isAtMost(percent: Int): Boolean = occupied.toLong() * 100 <= capacity.toLong() * percent
}
