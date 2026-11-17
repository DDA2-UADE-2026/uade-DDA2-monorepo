package com.uade.dda2.server.config

import jakarta.annotation.PostConstruct
import org.springframework.boot.context.properties.ConfigurationProperties
import java.net.URI
import java.time.Duration

@ConfigurationProperties("app.m1.auth")
class M1AuthProperties {
    var enabled = false
    var baseUrl = "https://ciudadanos-m1-backend.onrender.com"
    var connectTimeout: Duration = Duration.ofSeconds(5)
    var readTimeout: Duration = Duration.ofSeconds(15)

    @PostConstruct
    fun validate() {
        if (!enabled) return
        val uri = runCatching { URI.create(baseUrl) }.getOrNull()
        require(uri != null && uri.host != null && uri.userInfo == null && uri.query == null &&
            uri.fragment == null && uri.path.orEmpty().trimEnd('/').isEmpty() &&
            uri.port in -1..65535 && uri.port != 0 &&
            (uri.scheme == "https" || uri.scheme == "http" && uri.host in LOOPBACK_HOSTS)) {
            "M1_AUTH_BASE_URL requiere un origen HTTPS sin credenciales, query ni fragmento; HTTP solo en loopback para pruebas."
        }
        listOf(connectTimeout, readTimeout).forEach {
            require(it >= Duration.ofMillis(1) && it <= Duration.ofMillis(Int.MAX_VALUE.toLong())) {
                "Los timeouts de M1 deben estar entre 1 y ${Int.MAX_VALUE} milisegundos."
            }
        }
    }

    private companion object {
        val LOOPBACK_HOSTS = setOf("localhost", "127.0.0.1", "::1", "[::1]")
    }
}
