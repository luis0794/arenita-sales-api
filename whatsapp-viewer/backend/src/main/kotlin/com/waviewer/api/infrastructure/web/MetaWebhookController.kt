package com.waviewer.api.infrastructure.web

import com.waviewer.api.application.WebhookIngestService
import com.waviewer.api.application.WebhookSignatureValidator
import com.waviewer.api.infrastructure.config.MetaProperties
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Endpoint público que consume Meta. No usa la API key del visor: la autenticidad
 * se garantiza con el verify token (GET) y la firma HMAC X-Hub-Signature-256 (POST).
 */
@RestController
@RequestMapping("/api/v1/webhook")
class MetaWebhookController(
    private val props: MetaProperties,
    private val signatureValidator: WebhookSignatureValidator,
    private val ingestService: WebhookIngestService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /** Verificación inicial del webhook desde el panel de Meta. */
    @GetMapping
    fun verify(
        @RequestParam("hub.mode") mode: String?,
        @RequestParam("hub.verify_token") token: String?,
        @RequestParam("hub.challenge") challenge: String?,
    ): ResponseEntity<String> =
        if (mode == "subscribe" && token == props.verifyToken && challenge != null) {
            ResponseEntity.ok(challenge)
        } else {
            ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

    @PostMapping
    fun receive(
        @RequestBody rawBody: String,
        @RequestHeader(value = "X-Hub-Signature-256", required = false) signature: String?,
    ): ResponseEntity<Void> {
        if (!signatureValidator.isValid(rawBody.toByteArray(Charsets.UTF_8), signature)) {
            log.warn("Webhook con firma inválida descartado")
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
        runCatching { ingestService.ingest(rawBody) }
            .onFailure { log.error("Error procesando webhook; se responde 200 para evitar reintentos infinitos", it) }
        return ResponseEntity.ok().build()
    }
}
