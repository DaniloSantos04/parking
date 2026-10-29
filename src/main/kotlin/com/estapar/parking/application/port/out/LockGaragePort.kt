package com.estapar.parking.application.port.out

interface LockGaragePort {
    fun lockAndGetCapacity(): Int
}
