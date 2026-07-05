package com.waviewer.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class WaViewerApplication

fun main(args: Array<String>) {
    runApplication<WaViewerApplication>(*args)
}
