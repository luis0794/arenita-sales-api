package com.waviewer.api.application

import com.waviewer.api.infrastructure.config.MetaProperties
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

/**
 * Cliente mínimo de la Cloud API de Meta. La única operación que necesita el visor
 * es mark-as-read, y SOLO se invoca cuando el usuario lo pide explícitamente:
 * es la acción que envía las palomitas azules al remitente.
 */
@Component
class WhatsAppCloudClient(private val props: MetaProperties) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val client: RestClient = RestClient.builder()
        .baseUrl(props.graphApiBaseUrl)
        .defaultHeader("Authorization", "Bearer ${props.accessToken}")
        .build()

    /**
     * Marca un mensaje entrante como leído en WhatsApp (envía read receipt).
     * Meta también marca como leídos los mensajes anteriores de la conversación.
     */
    fun markAsRead(waMessageId: String) {
        val response = client.post()
            .uri("/{phoneNumberId}/messages", props.phoneNumberId)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                mapOf(
                    "messaging_product" to "whatsapp",
                    "status" to "read",
                    "message_id" to waMessageId,
                ),
            )
            .retrieve()
            .toBodilessEntity()
        log.info("mark-as-read {} -> {}", waMessageId, response.statusCode)
    }
}
