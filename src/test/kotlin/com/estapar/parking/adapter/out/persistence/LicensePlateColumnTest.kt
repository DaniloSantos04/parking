package com.estapar.parking.adapter.out.persistence

import com.estapar.parking.IntegrationTest
import com.estapar.parking.domain.model.LicensePlate
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import kotlin.test.assertEquals

/**
 * O SQL das migrations não consegue referenciar a constante do Kotlin, e o `ddl-auto: validate`
 * do Hibernate não confere tamanho de VARCHAR. Este teste é o que mantém os dois alinhados.
 */
@IntegrationTest
class LicensePlateColumnTest(
    @Autowired private val jdbcTemplate: JdbcTemplate,
) {

    @Test
    fun `colunas license_plate tem o tamanho de LicensePlate MAX_LENGTH`() {
        val lengths = jdbcTemplate.queryForList(
            """
            select table_name, column_name, character_maximum_length
            from information_schema.columns
            where table_schema = database() and column_name in ('license_plate', 'active_plate')
            """.trimIndent(),
        ).associate { "${it["TABLE_NAME"]}.${it["COLUMN_NAME"]}" to (it["CHARACTER_MAXIMUM_LENGTH"] as Number).toInt() }

        assertEquals(
            setOf("anomaly.license_plate", "parking_session.license_plate", "parking_session.active_plate"),
            lengths.keys,
        )
        lengths.forEach { (column, length) -> assertEquals(LicensePlate.MAX_LENGTH, length, "tamanho de $column") }
    }
}
