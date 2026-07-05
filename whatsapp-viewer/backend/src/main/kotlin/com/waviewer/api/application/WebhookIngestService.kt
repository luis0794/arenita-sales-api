package com.waviewer.api.application

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.waviewer.api.domain.model.Conversation
import com.waviewer.api.domain.model.Message
import com.waviewer.api.domain.model.MessageDirection
import com.waviewer.api.domain.repository.ConversationRepository
import com.waviewer.api.domain.repository.MessageRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Ingesta de webhooks de la Cloud API en modo coexistencia:
 *  - `messages`: mensajes entrantes de contactos (los que interesan al visor).
 *  - `smb_message_echoes`: mensajes que el dueño envía desde la app WhatsApp Business,
 *    espejados a la API. Se guardan para dar contexto al hilo.
 *
 * Recibir y almacenar estos eventos NO envía confirmaciones de lectura: las
 * palomitas azules solo se disparan con el endpoint mark-as-read de Meta.
 */
@Service
class WebhookIngestService(
    private val objectMapper: ObjectMapper,
    private val conversationRepository: ConversationRepository,
    private val messageRepository: MessageRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun ingest(rawBody: String) {
        val root = objectMapper.readTree(rawBody)
        root.path("entry").forEach { entry ->
            entry.path("changes").forEach { change ->
                val value = change.path("value")
                when (change.path("field").asText()) {
                    "messages" -> ingestInbound(value)
                    "smb_message_echoes" -> ingestEchoes(value)
                    "smb_app_state_sync" -> ingestStateSync(value)
                    else -> log.debug("Webhook field ignorado: {}", change.path("field").asText())
                }
            }
        }
    }

    private fun ingestInbound(value: JsonNode) {
        val names = value.path("contacts").associate {
            it.path("wa_id").asText() to it.path("profile").path("name").asText(null)
        }
        value.path("messages").forEach { msg ->
            val from = msg.path("from").asText()
            if (from.isBlank()) return@forEach
            storeMessage(
                waContactId = from,
                displayName = names[from],
                msg = msg,
                direction = MessageDirection.INBOUND,
            )
        }
    }

    private fun ingestEchoes(value: JsonNode) {
        value.path("message_echoes").forEach { msg ->
            val to = msg.path("to").asText()
            if (to.isBlank()) return@forEach
            storeMessage(
                waContactId = to,
                displayName = null,
                msg = msg,
                direction = MessageDirection.OUTBOUND_APP,
            )
        }
    }

    /** Sincronización de contactos desde la app Business: solo actualiza nombres. */
    private fun ingestStateSync(value: JsonNode) {
        value.path("state_sync").forEach { sync ->
            if (sync.path("type").asText() != "contact") return@forEach
            val contact = sync.path("contact")
            val waId = contact.path("phone_number").asText().removePrefix("+")
            val name = contact.path("full_name").asText(null)
            if (waId.isBlank() || name.isNullOrBlank()) return@forEach
            conversationRepository.findByWaContactId(waId)?.let {
                it.displayName = name
                conversationRepository.save(it)
            }
        }
    }

    private fun storeMessage(waContactId: String, displayName: String?, msg: JsonNode, direction: MessageDirection) {
        val waMessageId = msg.path("id").asText()
        if (waMessageId.isBlank() || messageRepository.existsByWaMessageId(waMessageId)) return

        val timestamp = msg.path("timestamp").asLong(0)
            .let { if (it > 0) Instant.ofEpochSecond(it) else Instant.now() }
        val type = msg.path("type").asText("unknown")
        val body = extractBody(msg, type)

        val conversation = conversationRepository.findByWaContactId(waContactId)
            ?: Conversation(waContactId = waContactId)

        displayName?.takeIf { it.isNotBlank() }?.let { conversation.displayName = it }
        if (timestamp >= conversation.lastMessageAt || conversation.id == null) {
            conversation.lastMessageAt = timestamp
            conversation.lastMessagePreview = body?.take(512) ?: "[$type]"
        }
        if (direction == MessageDirection.INBOUND) conversation.unreadCount++
        val saved = conversationRepository.save(conversation)

        messageRepository.save(
            Message(
                waMessageId = waMessageId,
                conversationId = saved.id!!,
                direction = direction,
                type = type,
                body = body,
                timestamp = timestamp,
            ),
        )
    }

    private fun extractBody(msg: JsonNode, type: String): String? = when (type) {
        "text" -> msg.path("text").path("body").asText(null)
        "image", "video", "document", "audio", "sticker" ->
            msg.path(type).path("caption").asText(null)
        "reaction" -> msg.path("reaction").path("emoji").asText(null)
        "location" -> msg.path("location").let {
            listOfNotNull(
                it.path("name").asText(null),
                it.path("address").asText(null),
            ).joinToString(" - ").ifBlank { null }
        }
        "contacts" -> msg.path("contacts").firstOrNull()
            ?.path("name")?.path("formatted_name")?.asText(null)
        else -> null
    }
}
