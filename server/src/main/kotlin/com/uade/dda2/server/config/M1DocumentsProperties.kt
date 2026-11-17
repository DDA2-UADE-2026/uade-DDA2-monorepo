package com.uade.dda2.server.config

import jakarta.annotation.PostConstruct
import org.springframework.boot.context.properties.ConfigurationProperties
import java.net.URI
import java.time.Duration

@ConfigurationProperties("app.m1.documents")
class M1DocumentsProperties {
    var enabled = false
    var baseUrl = "https://ciudadanos-m1-backend.onrender.com"
    // Sin valor predeterminado: M1 todavía debe acordar la consulta por UUID.
    var filePathTemplate = ""
    var serviceBearerToken = ""
    var connectTimeout: Duration = Duration.ofSeconds(5)
    var readTimeout: Duration = Duration.ofSeconds(15)
    var maxFileBytes = 10_485_760

    @PostConstruct
    fun validate() {
        if (!enabled) return
        val origin = runCatching { URI.create(baseUrl) }.getOrNull()
        require(origin != null && origin.host != null && origin.userInfo == null && origin.query == null &&
            origin.fragment == null && origin.path.orEmpty().trimEnd('/').isEmpty() &&
            origin.port in -1..65535 && origin.port != 0 &&
            (origin.scheme == "https" || origin.scheme == "http" && origin.host in setOf("localhost", "127.0.0.1", "[::1]", "::1"))) {
            "M1_DOCUMENTS_BASE_URL requiere un origen HTTPS; HTTP solo en loopback para pruebas."
        }
        val segments = filePathTemplate.removePrefix("/").split('/')
        require(filePathTemplate.startsWith('/') && segments.count { it == "{documentId}" } == 1 &&
            segments.all { it == "{documentId}" || it.matches(Regex("[A-Za-z0-9_.-]+")) && it !in setOf(".", "..") }) {
            "M1_DOCUMENTS_FILE_PATH_TEMPLATE requiere una ruta absoluta con un único segmento {documentId}, sin query ni fragmento."
        }
        require(serviceBearerToken.matches(Regex("[A-Za-z0-9._~+/-]+=*"))) {
            "M1_DOCUMENTS_SERVICE_BEARER_TOKEN requiere una credencial de servicio Bearer válida."
        }
        require(maxFileBytes in 1..10_485_760) { "M1_DOCUMENTS_MAX_FILE_BYTES debe estar entre 1 y 10485760." }
        listOf(connectTimeout, readTimeout).forEach {
            require(it >= Duration.ofMillis(1) && it <= Duration.ofMillis(Int.MAX_VALUE.toLong())) {
                "Los timeouts documentales de M1 deben estar entre 1 y ${Int.MAX_VALUE} milisegundos."
            }
        }
    }
}
