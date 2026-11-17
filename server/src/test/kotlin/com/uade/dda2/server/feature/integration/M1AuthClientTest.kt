package com.uade.dda2.server.feature.integration

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import com.uade.dda2.server.config.M1AuthConfiguration
import com.uade.dda2.server.config.M1AuthProperties
import com.uade.dda2.server.error.GlobalExceptionHandler
import com.uade.dda2.server.feature.integration.dto.M1LoginRequest
import com.uade.dda2.server.feature.integration.error.M1AuthException
import com.uade.dda2.server.feature.integration.error.M1AuthFailure
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.node.ObjectNode
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.*

class M1AuthClientTest {
    private val json = JsonMapper.builder().findAndAddModules().build()
    private val executor = Executors.newCachedThreadPool()
    private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
        this.executor = this@M1AuthClientTest.executor
        start()
    }
    private val properties = M1AuthProperties().apply {
        enabled = true
        baseUrl = "http://127.0.0.1:${server.address.port}"
        connectTimeout = Duration.ofSeconds(1)
        readTimeout = Duration.ofSeconds(2)
    }
    private val client get() = M1AuthConfiguration().m1AuthClient(properties, json)
    private val credentials = M1LoginRequest("ciudadano@example.invalid", "  Clave con espacios y ñ  ")
    private val requests = AtomicInteger()
    private val successBody = Files.readString(Path.of("docs/external/m1-auth/login-success.json"))
    private var handler: (HttpExchange) -> Unit = { response(it, 200, successBody) }

    init {
        server.createContext("/auth/usuario/login") {
            requests.incrementAndGet()
            handler(it)
        }
    }

    @AfterEach fun stop() {
        server.stop(0)
        executor.shutdownNow()
    }

    private fun response(exchange: HttpExchange, status: Int, body: String, authorization: String? = "Bearer test-access-token",
        contentType: String = "application/json") {
        exchange.responseHeaders.add("Content-Type", contentType)
        authorization?.let { exchange.responseHeaders.add("Authorization", it) }
        exchange.responseHeaders.add("X-Refresh-Token", "test-refresh-token")
        exchange.responseHeaders.add("X-Token-Expires-In", "900")
        val bytes = body.toByteArray(Charsets.UTF_8)
        exchange.sendResponseHeaders(status, if (status == 204) -1 else bytes.size.toLong())
        exchange.responseBody.use { if (status != 204) it.write(bytes) }
        exchange.close()
    }

    @Test fun `envia gmail y password una sola vez y devuelve identidad sin tokens ni permisos remotos`() {
        val received = AtomicReference<String>()
        val headers = AtomicReference<Map<String, String?>>()
        handler = {
            received.set(it.requestBody.bufferedReader(Charsets.UTF_8).use { reader -> reader.readText() })
            headers.set(mapOf("method" to it.requestMethod, "contentType" to it.requestHeaders.getFirst("Content-Type"),
                "authorization" to it.requestHeaders.getFirst("Authorization"), "cookie" to it.requestHeaders.getFirst("Cookie")))
            response(it, 200, successBody)
        }
        val result = client.login(credentials.copy(gmail = " ${credentials.gmail} "))
        val body = json.readTree(received.get())
        assertEquals(setOf("gmail", "password"), body.properties().map { it.key }.toSet())
        assertEquals(credentials.gmail, body["gmail"].asString())
        assertEquals(credentials.password, body["password"].asString())
        assertEquals("POST", headers.get()["method"])
        assertTrue(headers.get()["contentType"].orEmpty().startsWith("application/json"))
        assertNull(headers.get()["authorization"])
        assertNull(headers.get()["cookie"])
        assertEquals(UUID.fromString("b63f5a2e-4b71-46e7-8b01-381c85d88f62"), result.citizenId)
        assertEquals(8452L, result.userId)
        assertEquals("Marcos Ferreyra", result.name)
        val returned = json.readTree(json.writeValueAsString(result))
        assertEquals(setOf("citizenId", "userId", "name"), returned.properties().map { it.key }.toSet())
        assertEquals(1, requests.get())
    }

    @Test fun `acepta campos extra contactos vacios y UUID equivalentes en mayusculas`() {
        val body = json.readTree(successBody) as ObjectNode
        body.put("actorId", body["citizenId"].asString().uppercase())
        body.put("datoNuevo", "ignorar")
        body.put("nombre", "  Marcos  ")
        body.put("apellido", " Ferreyra ")
        handler = { response(it, 200, json.writeValueAsString(body)) }
        assertEquals("Marcos Ferreyra", client.login(credentials).name)
    }

    @Test fun `admite identificadores auxiliares ausentes y un solo componente de nombre`() {
        val body = json.readTree(successBody) as ObjectNode
        listOf("actorId", "uuid", "user_id", "apellido").forEach { body.remove(it) }
        handler = { response(it, 200, json.writeValueAsString(body)) }
        val result = client.login(credentials)
        assertNull(result.userId)
        assertEquals("Marcos", result.name)
    }

    @ParameterizedTest
    @ValueSource(strings = ["missing-auth", "string-auth", "numeric-auth", "missing-actor", "numeric-actor",
        "missing-citizen", "invalid-citizen", "short-uuid", "numeric-citizen", "wrong-actor-id", "wrong-uuid",
        "numeric-actor-id", "invalid-uuid", "numeric-uuid", "string-user-id", "overflow-user-id", "negative-user-id", "missing-name",
        "numeric-name", "long-name"])
    fun `rechaza respuestas ambiguas y no coerciona los datos de identidad`(scenario: String) {
        val body = json.readTree(successBody) as ObjectNode
        when (scenario) {
            "missing-auth" -> body.remove("auth")
            "string-auth" -> body.put("auth", "true")
            "numeric-auth" -> body.put("auth", 1)
            "missing-actor" -> body.remove("actorType")
            "numeric-actor" -> body.put("actorType", 1)
            "missing-citizen" -> body.remove("citizenId")
            "invalid-citizen" -> body.put("citizenId", "4")
            "short-uuid" -> body.put("citizenId", "1-1-1-1-1")
            "numeric-citizen" -> body.put("citizenId", 4)
            "wrong-actor-id" -> body.put("actorId", UUID.randomUUID().toString())
            "wrong-uuid" -> body.put("uuid", UUID.randomUUID().toString())
            "numeric-actor-id" -> body.put("actorId", 4)
            "invalid-uuid" -> body.put("uuid", "invalid")
            "numeric-uuid" -> body.put("uuid", 4)
            "string-user-id" -> body.put("user_id", "8452")
            "overflow-user-id" -> body.put("user_id", java.math.BigInteger("9223372036854775808"))
            "negative-user-id" -> body.put("user_id", -1)
            "missing-name" -> { body.putNull("nombre"); body.put("apellido", " ") }
            "numeric-name" -> body.put("nombre", 1)
            "long-name" -> body.put("nombre", "a".repeat(151))
        }
        handler = { response(it, 200, json.writeValueAsString(body)) }
        assertEquals(M1AuthFailure.INVALID_RESPONSE, assertFailsWith<M1AuthException> { client.login(credentials) }.failure)
        assertEquals(1, requests.get())
    }

    @ParameterizedTest
    @ValueSource(strings = ["no-authorization", "empty-bearer", "basic", "non-json", "invalid-json", "empty-body",
        "array", "trailing-json", "oversized-body", "created", "no-content"])
    fun `rechaza respuestas que no cumplen el protocolo acordado`(scenario: String) {
        handler = {
            when (scenario) {
                "no-authorization" -> response(it, 200, successBody, authorization = null)
                "empty-bearer" -> response(it, 200, successBody, authorization = "Bearer ")
                "basic" -> response(it, 200, successBody, authorization = "Basic secret")
                "non-json" -> response(it, 200, successBody, contentType = "text/html")
                "invalid-json" -> response(it, 200, "{invalid")
                "empty-body" -> response(it, 200, "")
                "array" -> response(it, 200, "[]")
                "trailing-json" -> response(it, 200, "$successBody {}")
                "oversized-body" -> response(it, 200, " ".repeat(65_537))
                "created" -> response(it, 201, successBody)
                "no-content" -> response(it, 204, "")
            }
        }
        assertEquals(M1AuthFailure.INVALID_RESPONSE, assertFailsWith<M1AuthException> { client.login(credentials) }.failure)
    }

    @Test fun `auth false rechaza las credenciales aun con HTTP 200`() {
        handler = { response(it, 200, """{"auth":false}""") }
        val error = assertFailsWith<M1AuthException> { client.login(credentials) }
        assertEquals(M1AuthFailure.INVALID_CREDENTIALS, error.failure)
        assertEquals(HttpStatus.UNAUTHORIZED, error.status)
    }

    @Test fun `actor no ciudadano no obtiene identidad aunque traiga role CIUDADANO`() {
        val body = json.readTree(successBody) as ObjectNode
        body.put("actorType", "EMPLEADO")
        handler = { response(it, 200, json.writeValueAsString(body)) }
        assertEquals(M1AuthFailure.ACTOR_NOT_ALLOWED, assertFailsWith<M1AuthException> { client.login(credentials) }.failure)
    }

    @ParameterizedTest
    @CsvSource("401,INVALID_CREDENTIALS", "403,ACCOUNT_FORBIDDEN", "429,RATE_LIMITED", "400,UPSTREAM_ERROR",
        "404,UPSTREAM_ERROR", "500,UPSTREAM_ERROR", "502,UPSTREAM_ERROR", "503,UNAVAILABLE", "504,UPSTREAM_ERROR")
    fun `traduce errores HTTP sin depender del cuerpo ni reintentar`(status: Int, expected: M1AuthFailure) {
        handler = { response(it, status, "<html>${credentials.password} test-access-token</html>") }
        val error = assertFailsWith<M1AuthException> { client.login(credentials) }
        assertEquals(expected, error.failure)
        assertEquals(expected.status, error.status)
        assertEquals(status, error.upstreamStatus)
        assertNull(error.cause)
        assertFalse(error.stackTraceToString().contains(credentials.password))
        assertFalse(error.stackTraceToString().contains("test-access-token"))
        assertEquals(1, requests.get())
    }

    @Test fun `error 401 documentado conserva el contrato publico de errores de M8`() {
        val body = Files.readString(Path.of("docs/external/m1-auth/login-invalid-credentials.json"))
        handler = { response(it, 401, body) }
        val error = assertFailsWith<M1AuthException> { client.login(credentials) }
        val response = GlobalExceptionHandler().handleApiException(error, MockHttpServletRequest("POST", "/future-login"))
        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        assertEquals("M1_AUTH_INVALID_CREDENTIALS", response.body?.code)
        assertEquals("/future-login", response.body?.path)
    }

    @Test fun `no sigue redirecciones con credenciales`() {
        val redirects = AtomicInteger()
        server.createContext("/redirected") { redirects.incrementAndGet(); response(it, 200, successBody) }
        handler = {
            it.responseHeaders.add("Location", "/redirected")
            response(it, 307, "")
        }
        assertEquals(M1AuthFailure.INVALID_RESPONSE, assertFailsWith<M1AuthException> { client.login(credentials) }.failure)
        assertEquals(0, redirects.get())
        assertEquals(1, requests.get())
    }

    @Test fun `timeout de lectura es distinto de credenciales invalidas y no reintenta`() {
        properties.readTimeout = Duration.ofMillis(150)
        handler = { Thread.sleep(750); runCatching { response(it, 200, successBody) } }
        val error = assertFailsWith<M1AuthException> { client.login(credentials) }
        assertEquals(M1AuthFailure.TIMEOUT, error.failure)
        assertEquals(HttpStatus.GATEWAY_TIMEOUT, error.status)
        assertEquals(1, requests.get())
    }

    @Test fun `conexion rechazada informa indisponibilidad sin conservar la causa`() {
        properties.baseUrl = "http://127.0.0.1:${ServerSocket(0).use { it.localPort }}"
        val error = assertFailsWith<M1AuthException> { client.login(credentials) }
        assertEquals(M1AuthFailure.UNAVAILABLE, error.failure)
        assertNull(error.cause)
    }

    @Test fun `timeout tambien cubre un cuerpo que se demora despues de los headers`() {
        properties.readTimeout = Duration.ofMillis(150)
        handler = {
            it.responseHeaders.add("Content-Type", "application/json")
            it.responseHeaders.add("Authorization", "Bearer test-access-token")
            it.sendResponseHeaders(200, successBody.toByteArray(Charsets.UTF_8).size.toLong())
            it.responseBody.flush()
            Thread.sleep(750)
            runCatching { it.responseBody.use { stream -> stream.write(successBody.toByteArray(Charsets.UTF_8)) } }
        }
        assertEquals(M1AuthFailure.TIMEOUT, assertFailsWith<M1AuthException> { client.login(credentials) }.failure)
        assertEquals(1, requests.get())
    }

    @Test fun `una conexion cerrada sin respuesta no reenvia el password`() {
        handler = { it.requestBody.close(); it.close() }
        assertEquals(M1AuthFailure.UNAVAILABLE, assertFailsWith<M1AuthException> { client.login(credentials) }.failure)
        assertEquals(1, requests.get())
    }

    @Test fun `credenciales vacias fallan antes de enviar y su toString no contiene secretos`() {
        assertFalse(credentials.toString().contains(credentials.gmail))
        assertFalse(credentials.toString().contains(credentials.password))
        listOf(credentials.copy(gmail = " "), credentials.copy(password = " ")).forEach {
            assertEquals(M1AuthFailure.INVALID_REQUEST, assertFailsWith<M1AuthException> { client.login(it) }.failure)
        }
        assertEquals(0, requests.get())
    }
}
