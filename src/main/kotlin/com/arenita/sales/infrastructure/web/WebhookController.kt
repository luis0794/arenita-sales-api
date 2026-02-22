package com.arenita.sales.infrastructure.web

import com.arenita.sales.api.dto.WebhookProformaEvent
import com.arenita.sales.application.ProformaService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/webhook")
@Tag(name = "Webhook", description = "External integrations via webhooks")
class WebhookController(private val proformaService: ProformaService) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping("/inbound")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Receive proforma events from external systems")
    fun receive(@RequestBody event: WebhookProformaEvent) {
        log.info("Webhook received: type=${event.eventType} company=${event.companyRuc} proforma=${event.proformaNumber}")
        // Process based on event type — extensible for each company's ERP
    }
}
