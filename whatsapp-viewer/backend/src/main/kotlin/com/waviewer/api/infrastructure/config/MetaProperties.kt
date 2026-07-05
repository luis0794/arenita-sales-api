package com.waviewer.api.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "meta")
data class MetaProperties(
    /** Token arbitrario que se configura igual en el panel de Meta (verificación del webhook) */
    val verifyToken: String,
    /** App Secret de la app de Meta; firma HMAC-SHA256 de cada webhook */
    val appSecret: String,
    /** System user access token con permiso whatsapp_business_messaging */
    val accessToken: String,
    /** Phone Number ID del número Business en la plataforma Cloud API */
    val phoneNumberId: String,
    val graphApiBaseUrl: String = "https://graph.facebook.com/v23.0",
)

@ConfigurationProperties(prefix = "viewer")
data class ViewerProperties(
    /** API key que la app iOS envía en el header X-Api-Key */
    val apiKey: String,
)
