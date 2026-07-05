package com.waviewer.api.application

import com.waviewer.api.api.dto.ConversationDto
import com.waviewer.api.api.dto.MessageDto
import com.waviewer.api.api.dto.SummaryDto
import com.waviewer.api.domain.model.MessageDirection
import com.waviewer.api.domain.repository.ConversationRepository
import com.waviewer.api.domain.repository.MessageRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class ConversationService(
    private val conversationRepository: ConversationRepository,
    private val messageRepository: MessageRepository,
    private val cloudClient: WhatsAppCloudClient,
) {

    @Transactional(readOnly = true)
    fun listConversations(onlyUnread: Boolean): List<ConversationDto> {
        val conversations = if (onlyUnread) {
            conversationRepository.findByUnreadCountGreaterThanOrderByLastMessageAtDesc(0)
        } else {
            conversationRepository.findAllByOrderByLastMessageAtDesc()
        }
        return conversations.map { ConversationDto.from(it) }
    }

    @Transactional(readOnly = true)
    fun listMessages(conversationId: Long, limit: Int): List<MessageDto> {
        requireConversation(conversationId)
        return messageRepository
            .findByConversationIdOrderByTimestampDesc(conversationId, PageRequest.of(0, limit.coerceIn(1, 200)))
            .map { MessageDto.from(it) }
    }

    /**
     * Marca la conversación como vista DENTRO del visor. Solo afecta a nuestra BD:
     * WhatsApp sigue mostrando el chat como no leído y el remitente no ve palomitas azules.
     */
    @Transactional
    fun markReadLocal(conversationId: Long) {
        val conversation = requireConversation(conversationId)
        conversation.unreadCount = 0
        conversationRepository.save(conversation)
    }

    /**
     * Acción EXPLÍCITA que envía la confirmación de lectura real a WhatsApp
     * (palomitas azules) usando el último mensaje entrante del hilo.
     */
    @Transactional
    fun markReadUpstream(conversationId: Long) {
        val conversation = requireConversation(conversationId)
        val lastInbound = messageRepository
            .findFirstByConversationIdAndDirectionOrderByTimestampDesc(conversationId, MessageDirection.INBOUND)
            ?: throw ResponseStatusException(
                org.springframework.http.HttpStatus.CONFLICT,
                "La conversación no tiene mensajes entrantes",
            )
        cloudClient.markAsRead(lastInbound.waMessageId)
        conversation.unreadCount = 0
        conversationRepository.save(conversation)
    }

    /** Resumen liviano para el widget de pantalla de inicio. */
    @Transactional(readOnly = true)
    fun summary(): SummaryDto {
        val unread = conversationRepository.findByUnreadCountGreaterThanOrderByLastMessageAtDesc(0)
        return SummaryDto(
            totalUnreadMessages = unread.sumOf { it.unreadCount },
            unreadConversations = unread.size,
            topConversations = unread.take(3).map { ConversationDto.from(it) },
        )
    }

    private fun requireConversation(id: Long) = conversationRepository.findById(id).orElseThrow {
        ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Conversación $id no existe")
    }
}
