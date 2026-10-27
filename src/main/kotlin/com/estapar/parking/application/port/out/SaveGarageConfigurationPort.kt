package com.estapar.parking.application.port.out

import com.estapar.parking.domain.model.GarageConfiguration

/**
 * Grava setores, vagas e capacidade da garagem de forma idempotente: chamar de novo com a
 * mesma configuração não duplica nada, e a ocupação atual das vagas é preservada.
 */
interface SaveGarageConfigurationPort {
    fun save(configuration: GarageConfiguration)
}
