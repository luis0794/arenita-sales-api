package com.waviewer.api.application

import com.waviewer.api.infrastructure.config.MetaProperties
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class WebhookSignatureValidatorTest {

    private val props = MetaProperties(
        verifyToken = "vt",
        appSecret = "test-secret",
        accessToken = "at",
        phoneNumberId = "123",
    )
    private val validator = WebhookSignatureValidator(props)

    private fun sign(body: String, secret: String = "test-secret"): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(), "HmacSHA256"))
        val hex = mac.doFinal(body.toByteArray()).joinToString("") { "%02x".format(it) }
        return "sha256=$hex"
    }

    @Test
    fun `acepta firma valida`() {
        val body = """{"object":"whatsapp_business_account"}"""
        assertTrue(validator.isValid(body.toByteArray(), sign(body)))
    }

    @Test
    fun `rechaza firma con secreto incorrecto`() {
        val body = """{"object":"whatsapp_business_account"}"""
        assertFalse(validator.isValid(body.toByteArray(), sign(body, secret = "otro-secreto")))
    }

    @Test
    fun `rechaza header ausente o malformado`() {
        val body = "{}".toByteArray()
        assertFalse(validator.isValid(body, null))
        assertFalse(validator.isValid(body, ""))
        assertFalse(validator.isValid(body, "md5=abc"))
    }
}
