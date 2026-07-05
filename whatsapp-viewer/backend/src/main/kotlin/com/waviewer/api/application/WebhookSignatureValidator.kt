package com.waviewer.api.application

import com.waviewer.api.infrastructure.config.MetaProperties
import org.springframework.stereotype.Component
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Valida el header X-Hub-Signature-256 que Meta envía en cada webhook:
 * "sha256=" + HMAC-SHA256(appSecret, rawBody).
 */
@Component
class WebhookSignatureValidator(private val props: MetaProperties) {

    fun isValid(rawBody: ByteArray, signatureHeader: String?): Boolean {
        if (signatureHeader.isNullOrBlank() || !signatureHeader.startsWith(PREFIX)) return false
        val expected = hmacSha256Hex(rawBody)
        val received = signatureHeader.removePrefix(PREFIX)
        return MessageDigest.isEqual(expected.toByteArray(), received.lowercase().toByteArray())
    }

    private fun hmacSha256Hex(payload: ByteArray): String {
        val mac = Mac.getInstance(ALGORITHM)
        mac.init(SecretKeySpec(props.appSecret.toByteArray(), ALGORITHM))
        return mac.doFinal(payload).joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val PREFIX = "sha256="
        private const val ALGORITHM = "HmacSHA256"
    }
}
