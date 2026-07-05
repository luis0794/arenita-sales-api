package com.waviewer.api.domain.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant

enum class MessageDirection {
    /** Recibido del contacto (webhook `messages`) */
    INBOUND,

    /** Enviado por el dueño desde la app WhatsApp Business (webhook `smb_message_echoes`) */
    OUTBOUND_APP,
}

@Entity
@Table(
    name = "messages",
    indexes = [
        Index(name = "idx_messages_conversation", columnList = "conversationId,timestamp"),
        Index(name = "idx_messages_wa_id", columnList = "waMessageId", unique = true),
    ],
)
class Message(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    /** wamid de Meta; se usa como clave de idempotencia y para mark-as-read */
    @Column(nullable = false)
    var waMessageId: String,

    @Column(nullable = false)
    var conversationId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var direction: MessageDirection,

    /** text, image, audio, video, document, sticker, location, contacts, reaction, unknown */
    @Column(nullable = false)
    var type: String,

    /** Texto del mensaje o caption del adjunto */
    @Column(length = 8192)
    var body: String? = null,

    var timestamp: Instant,

    /** Visto en el visor. Local: jamás se propaga a WhatsApp. */
    var readLocally: Boolean = false,
)
