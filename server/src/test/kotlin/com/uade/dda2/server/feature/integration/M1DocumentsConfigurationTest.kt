package com.uade.dda2.server.feature.integration

import com.uade.dda2.server.config.M1DocumentsConfiguration
import com.uade.dda2.server.config.M1DocumentsProperties
import com.uade.dda2.server.feature.integration.client.M1DocumentClient
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import java.time.Duration
import kotlin.test.*

class M1DocumentsConfigurationTest {
    private val context = ApplicationContextRunner().withUserConfiguration(M1DocumentsConfiguration::class.java)
    private fun valid() = M1DocumentsProperties().apply {
        enabled = true
        baseUrl = "http://127.0.0.1:1"
        filePathTemplate = "/test-only/documents/{documentId}/file"
        serviceBearerToken = "test-service-token"
    }

    @Test fun `desactivado no crea cliente aunque falte el acuerdo de acceso`() {
        context.run {
            assertNull(it.startupFailure)
            assertTrue(it.getBeansOfType(M1DocumentClient::class.java).isEmpty())
        }
    }

    @Test fun `activado exige ruta y credencial y no conecta al arrancar`() {
        context.withPropertyValues("app.m1.documents.enabled=true").run { assertNotNull(it.startupFailure) }
        context.withPropertyValues("app.m1.documents.enabled=true", "app.m1.documents.base-url=http://127.0.0.1:1",
            "app.m1.documents.file-path-template=/test-only/documents/{documentId}/file",
            "app.m1.documents.service-bearer-token=test-service-token").run {
            assertNull(it.startupFailure)
            assertNotNull(it.getBean(M1DocumentClient::class.java))
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "//evil.invalid/{documentId}", "https://evil.invalid/{documentId}", "/docs/../{documentId}",
        "/docs/%2e%2e/{documentId}", "/docs/{documentId}?token=x", "/docs/{documentId}#fragment", "/docs/{id}/file",
        "/docs/{documentId}/{documentId}", "/docs/prefix-{documentId}"])
    fun `ruta relativa al origen no puede cambiar destino ni consultar por otro identificador`(path: String) {
        assertFailsWith<IllegalArgumentException> { valid().apply { filePathTemplate = path }.validate() }
    }

    @ParameterizedTest
    @ValueSource(strings = ["http://example.invalid", "https://user:secret@example.invalid", "https://example.invalid/path",
        "https://example.invalid?secret=x", "https://example.invalid#fragment", "file:///tmp/file", "https://example.invalid:0"])
    fun `rechaza origen inseguro o con datos adicionales`(url: String) {
        assertFailsWith<IllegalArgumentException> { valid().apply { baseUrl = url }.validate() }
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "Bearer token", "token\r\nX-Evil: yes", " token", "token "])
    fun `credencial requerida sin prefijo espacios ni inyeccion de encabezados`(token: String) {
        assertFailsWith<IllegalArgumentException> { valid().apply { serviceBearerToken = token }.validate() }
    }

    @Test fun `limites de memoria y plazos son obligatorios`() {
        valid().validate()
        for (size in listOf(-1, 0, 10_485_761, Int.MAX_VALUE))
            assertFailsWith<IllegalArgumentException> { valid().apply { maxFileBytes = size }.validate() }
        for (timeout in listOf(Duration.ZERO, Duration.ofNanos(1), Duration.ofDays(30))) {
            assertFailsWith<IllegalArgumentException> { valid().apply { connectTimeout = timeout }.validate() }
            assertFailsWith<IllegalArgumentException> { valid().apply { readTimeout = timeout }.validate() }
        }
    }
}
