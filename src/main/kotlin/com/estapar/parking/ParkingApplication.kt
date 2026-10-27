package com.estapar.parking

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class ParkingApplication

fun main(args: Array<String>) {
    runApplication<ParkingApplication>(*args)
}
