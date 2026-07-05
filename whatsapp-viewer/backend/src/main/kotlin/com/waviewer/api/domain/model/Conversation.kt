package com.waviewer.api.domain.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

/**
 * Un chat 1:1 con un contacto de WhatsApp. La coexistencia de Meta no sincroniza
 * grupos, por lo que solo existen conversaciones individuales.
 */
@Entity
@Table(name = "conversations")
class Conversation(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    /** Número E.164 del contacto (wa_id de Meta), p.ej. 5939XXXXXXXX */
    @Column(unique = true, nullable = false)
    var waContactId: String,

    var displayName: String? = null,

    var lastMessageAt: Instant = Instant.now(),

    @Column(length = 512)
    var lastMessagePreview: String? = null,

    /** Mensajes entrantes aún no vistos en el visor. Nunca dispara read receipts. */
    var unreadCount: Int = 0,
)
