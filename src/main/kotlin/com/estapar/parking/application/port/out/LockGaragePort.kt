package com.estapar.parking.application.port.out

/**
 * Lock pessimista na garagem (SELECT ... FOR UPDATE na linha única), mantido até o fim da
 * transação corrente. Serializa os eventos que mudam a lotação ou a ocupação das vagas.
 */
interface LockGaragePort {
    /** Bloqueia a garagem e devolve a capacidade total. */
    fun lockAndGetCapacity(): Int
}
