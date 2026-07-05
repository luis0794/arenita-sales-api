package com.waviewer.api.application

import com.waviewer.api.domain.model.MessageDirection
import com.waviewer.api.domain.repository.ConversationRepository
import com.waviewer.api.domain.repository.MessageRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("dev")
class WebhookIngestServiceTest {

    @Autowired lateinit var ingest: WebhookIngestService
    @Autowired lateinit var conversations: ConversationRepository
    @Autowired lateinit var messages: MessageRepository

    @BeforeEach
    fun clean() {
        messages.deleteAll()
        conversations.deleteAll()
    }

    private val inboundPayload = """
        {
          "object": "whatsapp_business_account",
          "entry": [{
            "id": "WABA_ID",
            "changes": [{
              "field": "messages",
              "value": {
                "messaging_product": "whatsapp",
                "metadata": {"display_phone_number": "593999999999", "phone_number_id": "PNID"},
                "contacts": [{"profile": {"name": "Juan Perez"}, "wa_id": "593988888888"}],
                "messages": [{
                  "from": "593988888888",
                  "id": "wamid.ABC123",
                  "timestamp": "1720180800",
                  "type": "text",
                  "text": {"body": "Hola, necesito una cotización"}
                }]
              }
            }]
          }]
        }
    """.trimIndent()

    @Test
    fun `ingesta mensaje entrante y crea conversacion no leida`() {
        ingest.ingest(inboundPayload)

        val convo = conversations.findByWaContactId("593988888888")
        assertNotNull(convo)
        assertEquals("Juan Perez", convo!!.displayName)
        assertEquals(1, convo.unreadCount)
        assertEquals("Hola, necesito una cotización", convo.lastMessagePreview)

        val stored = messages.findAll().single()
        assertEquals(MessageDirection.INBOUND, stored.direction)
        assertEquals("wamid.ABC123", stored.waMessageId)
        assertEquals(false, stored.readLocally)
    }

    @Test
    fun `webhook duplicado es idempotente`() {
        ingest.ingest(inboundPayload)
        ingest.ingest(inboundPayload)

        assertEquals(1, messages.count())
        assertEquals(1, conversations.findByWaContactId("593988888888")!!.unreadCount)
    }

    @Test
    fun `echo desde la app Business no incrementa no leidos`() {
        ingest.ingest(inboundPayload)
        ingest.ingest(
            """
            {
              "object": "whatsapp_business_account",
              "entry": [{
                "changes": [{
                  "field": "smb_message_echoes",
                  "value": {
                    "messaging_product": "whatsapp",
                    "message_echoes": [{
                      "from": "593999999999",
                      "to": "593988888888",
                      "id": "wamid.ECHO1",
                      "timestamp": "1720184400",
                      "type": "text",
                      "text": {"body": "Claro, te la envío"}
                    }]
                  }
                }]
              }]
            }
            """.trimIndent(),
        )

        val convo = conversations.findByWaContactId("593988888888")!!
        assertEquals(1, convo.unreadCount)
        assertEquals("Claro, te la envío", convo.lastMessagePreview)
        assertEquals(2, messages.count())
        assertEquals(
            MessageDirection.OUTBOUND_APP,
            messages.findAll().single { it.waMessageId == "wamid.ECHO1" }.direction,
        )
    }
}
