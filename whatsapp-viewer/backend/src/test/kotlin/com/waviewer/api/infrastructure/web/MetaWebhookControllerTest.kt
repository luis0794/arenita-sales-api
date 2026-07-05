package com.waviewer.api.infrastructure.web

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class MetaWebhookControllerTest {

    @Autowired lateinit var mockMvc: MockMvc

    // Debe coincidir con los defaults de application.yml
    private val appSecret = "dev-app-secret"
    private val verifyToken = "dev-verify-token"

    private fun sign(body: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(appSecret.toByteArray(), "HmacSHA256"))
        return "sha256=" + mac.doFinal(body.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    @Test
    fun `verificacion GET devuelve el challenge`() {
        mockMvc.get("/api/v1/webhook") {
            param("hub.mode", "subscribe")
            param("hub.verify_token", verifyToken)
            param("hub.challenge", "12345")
        }.andExpect {
            status { isOk() }
            content { string("12345") }
        }
    }

    @Test
    fun `verificacion GET con token incorrecto devuelve 403`() {
        mockMvc.get("/api/v1/webhook") {
            param("hub.mode", "subscribe")
            param("hub.verify_token", "token-malo")
            param("hub.challenge", "12345")
        }.andExpect { status { isForbidden() } }
    }

    @Test
    fun `POST con firma valida devuelve 200`() {
        val body = """{"object":"whatsapp_business_account","entry":[]}"""
        mockMvc.post("/api/v1/webhook") {
            contentType = org.springframework.http.MediaType.APPLICATION_JSON
            content = body
            header("X-Hub-Signature-256", sign(body))
        }.andExpect { status { isOk() } }
    }

    @Test
    fun `POST sin firma devuelve 403`() {
        mockMvc.post("/api/v1/webhook") {
            contentType = org.springframework.http.MediaType.APPLICATION_JSON
            content = """{"object":"whatsapp_business_account","entry":[]}"""
        }.andExpect { status { isForbidden() } }
    }

    @Test
    fun `API del visor exige API key`() {
        mockMvc.get("/api/v1/conversations").andExpect { status { isUnauthorized() } }
        mockMvc.get("/api/v1/conversations") {
            header("X-Api-Key", "dev-api-key-change-me")
        }.andExpect { status { isOk() } }
    }
}
