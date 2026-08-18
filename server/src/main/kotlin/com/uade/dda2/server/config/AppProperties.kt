package com.uade.dda2.server.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("app.security.jwt")
data class JwtProperties(
    var issuer: String = "social-development-api",
    var secret: String = "change-me-change-me-change-me-change-me-32chars",
    var expirationSeconds: Long = 28_800,
    var cookieName: String = "access_token",
    // false solo hace falta en dev/CI sobre HTTP con un host que no sea "localhost"
    // (los browsers tratan "localhost" como contexto seguro y sí mandan cookies Secure ahí).
    var cookieSecure: Boolean = true,
)

@ConfigurationProperties("app.cors")
data class CorsProperties(
    var allowedOrigins: List<String> = listOf("http://localhost:3000", "http://127.0.0.1:3000"),
)
