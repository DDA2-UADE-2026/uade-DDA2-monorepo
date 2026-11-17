package com.uade.dda2.server.feature.integration

import com.uade.dda2.server.config.M1AuthConfiguration
import com.uade.dda2.server.config.M1AuthProperties
import com.uade.dda2.server.feature.integration.client.M1AuthClient
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import tools.jackson.databind.json.JsonMapper
import java.time.Duration
import kotlin.test.*

class M1AuthConfigurationTest {
    private val context = ApplicationContextRunner()
        .withUserConfiguration(M1AuthConfiguration::class.java)
        .withBean(JsonMapper::class.java, { JsonMapper.builder().findAndAddModules().build() })

    @Test fun `cliente desactivado por defecto no crea objetos HTTP`() {
        context.run {
            assertNull(it.startupFailure)
            assertTrue(it.getBeansOfType(M1AuthClient::class.java).isEmpty())
        }
    }

    @Test fun `cliente activado arranca sin contactar a M1 y vincula propiedades`() {
        context.withPropertyValues("app.m1.auth.enabled=true", "app.m1.auth.base-url=http://127.0.0.1:1",
            "app.m1.auth.connect-timeout=2s", "app.m1.auth.read-timeout=3s").run {
            assertNull(it.startupFailure)
            assertNotNull(it.getBean(M1AuthClient::class.java))
            val properties = it.getBean(M1AuthProperties::class.java)
            assertEquals(Duration.ofSeconds(2), properties.connectTimeout)
            assertEquals(Duration.ofSeconds(3), properties.readTimeout)
        }
    }

    @Test fun `configuracion invalida impide activar el cliente`() {
        context.withPropertyValues("app.m1.auth.enabled=true", "app.m1.auth.base-url=http://example.com").run {
            assertNotNull(it.startupFailure)
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["https://ciudadanos-m1-backend.onrender.com", "https://example.com/",
        "http://127.0.0.1:8080", "http://localhost:8080", "http://[::1]:8080"])
    fun `permite HTTPS remoto y HTTP local para pruebas`(url: String) {
        M1AuthProperties().apply { enabled = true; baseUrl = url }.validate()
    }

    @ParameterizedTest
    @ValueSource(strings = ["http://example.com", "https://user:secret@example.com", "https://example.com/path",
        "https://example.com?secret=value", "https://example.com#fragment", "file:///tmp/login", "relative",
        "https:///missing-host", "https://example.com:0", "https://example.com:65536", "not a URL"])
    fun `rechaza origenes que podrian enviar credenciales a un destino incorrecto`(url: String) {
        assertFailsWith<IllegalArgumentException> { M1AuthProperties().apply { enabled = true; baseUrl = url }.validate() }
    }

    @Test fun `timeouts positivos y acotados son obligatorios al activar`() {
        val invalid = listOf(Duration.ZERO, Duration.ofMillis(-1), Duration.ofNanos(1), Duration.ofDays(100_000_000))
        invalid.forEach { timeout ->
            assertFailsWith<IllegalArgumentException> { M1AuthProperties().apply { enabled = true; connectTimeout = timeout }.validate() }
            assertFailsWith<IllegalArgumentException> { M1AuthProperties().apply { enabled = true; readTimeout = timeout }.validate() }
        }
        M1AuthProperties().apply { enabled = true }.validate()
    }
}
