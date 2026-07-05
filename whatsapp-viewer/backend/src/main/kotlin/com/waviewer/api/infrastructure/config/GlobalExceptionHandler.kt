package com.waviewer.api.infrastructure.config

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ResponseStatusException
import java.time.Instant

data class ApiError(val status: Int, val message: String?, val timestamp: Instant = Instant.now())

@RestControllerAdvice
class GlobalExceptionHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(ResponseStatusException::class)
    fun handleStatus(ex: ResponseStatusException): ResponseEntity<ApiError> =
        ResponseEntity.status(ex.statusCode).body(ApiError(ex.statusCode.value(), ex.reason))

    @ExceptionHandler(Exception::class)
    fun handleGeneric(ex: Exception): ResponseEntity<ApiError> {
        log.error("Error no controlado", ex)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiError(500, "Error interno"))
    }
}
