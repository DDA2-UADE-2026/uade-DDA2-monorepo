package com.uade.dda2.server.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.ZoneId

@ConfigurationProperties("app.security.jwt")
data class JwtProperties(
    var issuer: String = "social-development-api",
    var secret: String = "change-me-change-me-change-me-change-me-32chars",
    var expirationSeconds: Long = 28_800,
)

@ConfigurationProperties("app.cors")
data class CorsProperties(
    var allowedOrigins: List<String> = listOf("http://localhost:3000", "http://127.0.0.1:3000"),
) {
    val nonBlankOrigins: List<String>
        get() = allowedOrigins.filter { it.isNotBlank() }
}

@ConfigurationProperties("app.enrollment-period.expiration")
data class EnrollmentPeriodExpirationProperties(
    var cron: String = "0 5 0 * * *",
    var zoneId: String = "America/Argentina/Buenos_Aires",
) {
    fun zone(): ZoneId = ZoneId.of(zoneId)
}
