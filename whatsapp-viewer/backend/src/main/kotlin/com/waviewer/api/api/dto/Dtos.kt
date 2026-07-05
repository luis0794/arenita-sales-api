package com.waviewer.api.api.dto

import com.waviewer.api.domain.model.Conversation
import com.waviewer.api.domain.model.Message
import java.time.Instant

data class ConversationDto(
    val id: Long,
    val waContactId: String,
    val displayName: String?,
    val lastMessageAt: Instant,
    val lastMessagePreview: String?,
    val unreadCount: Int,
) {
    companion object {
        fun from(c: Conversation) = ConversationDto(
            id = c.id!!,
            waContactId = c.waContactId,
            displayName = c.displayName,
            lastMessageAt = c.lastMessageAt,
            lastMessagePreview = c.lastMessagePreview,
            unreadCount = c.unreadCount,
        )
    }
}

data class MessageDto(
    val id: Long,
    val waMessageId: String,
    val direction: String,
    val type: String,
    val body: String?,
    val timestamp: Instant,
) {
    companion object {
        fun from(m: Message) = MessageDto(
            id = m.id!!,
            waMessageId = m.waMessageId,
            direction = m.direction.name,
            type = m.type,
            body = m.body,
            timestamp = m.timestamp,
        )
    }
}

data class SummaryDto(
    val totalUnreadMessages: Int,
    val unreadConversations: Int,
    val topConversations: List<ConversationDto>,
)
