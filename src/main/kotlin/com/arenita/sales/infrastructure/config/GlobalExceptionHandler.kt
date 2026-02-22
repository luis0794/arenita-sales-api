package com.arenita.sales.infrastructure.config

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    private val log = LoggerFactory.getLogger(javaClass)
    data class ErrorResponse(val error: String, val message: String?)

    @ExceptionHandler(NoSuchElementException::class)
    fun notFound(ex: NoSuchElementException) =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse("NOT_FOUND", ex.message))

    @ExceptionHandler(IllegalArgumentException::class)
    fun badRequest(ex: IllegalArgumentException) =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse("BAD_REQUEST", ex.message))

    @ExceptionHandler(Exception::class)
    fun general(ex: Exception): ResponseEntity<ErrorResponse> {
        log.error("Unhandled", ex)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse("INTERNAL_ERROR", ex.message))
    }
}
