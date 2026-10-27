package com.estapar.parking.domain.model

/**
 * Configuração da garagem fornecida pelo simulador: setores e vagas.
 *
 * A capacidade da garagem é a soma das capacidades dos setores. A lotação é controlada
 * para a garagem inteira, porque as cancelas ficam na entrada e os setores são só lógicos.
 */
data class GarageConfiguration(
    val sectors: List<Sector>,
    val spots: List<Spot>,
) {
    val capacity: Int = sectors.sumOf { it.maxCapacity }

    init {
        require(sectors.isNotEmpty()) { "A garagem precisa de ao menos um setor" }
        requireNoDuplicates(sectors.map { it.code }) { "Setores duplicados: $it" }
        requireNoDuplicates(spots.map { it.id }) { "Vagas com id duplicado: $it" }
        requireNoDuplicates(spots.map { it.coordinates }) { "Vagas com coordenadas duplicadas: $it" }

        val sectorCodes = sectors.map { it.code }.toSet()
        val orphanSpots = spots.filter { it.sectorCode !in sectorCodes }.map { it.id }
        require(orphanSpots.isEmpty()) { "Vagas de setores inexistentes: $orphanSpots" }
    }

    private fun <T> requireNoDuplicates(values: List<T>, message: (Set<T>) -> String) {
        val duplicates = values.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        require(duplicates.isEmpty()) { message(duplicates) }
    }
}
