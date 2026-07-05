package com.waviewer.api.domain.repository

import com.waviewer.api.domain.model.Conversation
import com.waviewer.api.domain.model.Message
import com.waviewer.api.domain.model.MessageDirection
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface ConversationRepository : JpaRepository<Conversation, Long> {
    fun findByWaContactId(waContactId: String): Conversation?
    fun findByUnreadCountGreaterThanOrderByLastMessageAtDesc(min: Int): List<Conversation>
    fun findAllByOrderByLastMessageAtDesc(): List<Conversation>
}

interface MessageRepository : JpaRepository<Message, Long> {
    fun existsByWaMessageId(waMessageId: String): Boolean
    fun findByConversationIdOrderByTimestampDesc(conversationId: Long, pageable: Pageable): List<Message>
    fun findFirstByConversationIdAndDirectionOrderByTimestampDesc(
        conversationId: Long,
        direction: MessageDirection,
    ): Message?
    fun countByDirectionAndReadLocallyFalse(direction: MessageDirection): Long
}
