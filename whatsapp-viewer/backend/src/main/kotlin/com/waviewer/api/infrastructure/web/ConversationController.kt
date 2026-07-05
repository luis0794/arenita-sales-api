package com.waviewer.api.infrastructure.web

import com.waviewer.api.api.dto.ConversationDto
import com.waviewer.api.api.dto.MessageDto
import com.waviewer.api.api.dto.SummaryDto
import com.waviewer.api.application.ConversationService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/** API que consume la app iOS (autenticada con X-Api-Key). */
@RestController
@RequestMapping("/api/v1")
class ConversationController(private val service: ConversationService) {

    @GetMapping("/conversations")
    fun conversations(@RequestParam(defaultValue = "false") onlyUnread: Boolean): List<ConversationDto> =
        service.listConversations(onlyUnread)

    @GetMapping("/conversations/{id}/messages")
    fun messages(@PathVariable id: Long, @RequestParam(defaultValue = "50") limit: Int): List<MessageDto> =
        service.listMessages(id, limit)

    /** Visto en el visor: no toca WhatsApp, el remitente no se entera. */
    @PostMapping("/conversations/{id}/read-local")
    fun readLocal(@PathVariable id: Long) = service.markReadLocal(id)

    /** Envía la confirmación de lectura REAL (palomitas azules) vía Cloud API. */
    @PostMapping("/conversations/{id}/read-upstream")
    fun readUpstream(@PathVariable id: Long) = service.markReadUpstream(id)

    /** Resumen para el widget de pantalla de inicio. */
    @GetMapping("/summary")
    fun summary(): SummaryDto = service.summary()
}
