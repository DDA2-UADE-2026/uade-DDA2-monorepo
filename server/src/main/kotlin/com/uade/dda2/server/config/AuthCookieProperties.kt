package com.uade.dda2.server.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("app.security.cookie")
data class AuthCookieProperties(
    val secure: Boolean = false,
    val sameSite: String = "Lax",
) {
    init {
        require(sameSite in setOf("Lax", "Strict", "None")) { "AUTH_COOKIE_SAME_SITE must be Lax, Strict or None." }
        require(sameSite != "None" || secure) { "SameSite=None requires AUTH_COOKIE_SECURE=true." }
    }
}
