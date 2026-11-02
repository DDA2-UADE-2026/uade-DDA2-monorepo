package com.uade.dda2.server.feature.integration

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import com.uade.dda2.server.config.CoreProperties
import com.uade.dda2.server.feature.integration.client.*
import com.uade.dda2.server.feature.integration.dto.CoreEventStatus
import com.uade.dda2.server.feature.integration.error.CoreHttpException
import com.uade.dda2.server.feature.integration.error.CoreIntegrationException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.web.client.RestClient
import tools.jackson.databind.json.JsonMapper
import java.net.InetSocketAddress
import java.time.Clock
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.*

class CoreHttpClientTest {
    private val json = JsonMapper.builder().findAndAddModules().build()
    private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply { start() }
    private val properties = CoreProperties().apply {
        machineSecret = "test-private-secret"
        apiBaseUrl = "http://127.0.0.1:${server.address.port}/api/v1"
    }
    private val transport = CoreHttpTransport(RestClient.create(), json, properties.apiBaseUrl)
    private val tokens = CoreTokenClient(transport, json, properties, Clock.systemUTC())
    private val registry = CoreRegistryClient(CoreApiClient(transport, tokens), json, properties)

    @AfterEach fun stop() { server.stop(0) }

    private fun response(exchange: HttpExchange, status: Int, body: String) {
        val bytes = body.toByteArray(Charsets.UTF_8)
        exchange.responseHeaders.add("Content-Type", "application/json")
        exchange.sendResponseHeaders(status, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }

    private fun tokenEndpoint(counter: AtomicInteger) {
        server.createContext("/api/v1/auth/module-token") { request ->
            assertEquals("test-private-secret", json.readTree(request.requestBody.readAllBytes()).get("secret").asString())
            val n = counter.incrementAndGet()
            response(request, 200, """{"accessToken":"token-$n","expiresIn":900,"module":"desarrollo-social",
                "kind":"module","tokenType":"Bearer","displayName":"Desarrollo Social","isAdmin":false,"actor":"module:desarrollo-social"}""")
        }
    }

    @Test fun `renueva solo una vez ante 401 y filtra publicaciones globales`() {
        val auth = AtomicInteger()
        val calls = AtomicInteger()
        tokenEndpoint(auth)
        val ownId = UUID.randomUUID()
        server.createContext("/api/v1/publications") { request ->
            calls.incrementAndGet()
            if (request.requestHeaders.getFirst("Authorization") == "Bearer token-1") response(request, 401, """{"code":"TOKEN_EXPIRED"}""")
            else response(request, 200, """[
                {"id":"$ownId","moduleName":"desarrollo-social","eventType":"probe","active":true},
                {"id":"${UUID.randomUUID()}","moduleName":"ciudadanos","eventType":"citizenUpdated","active":true}]""")
        }
        assertEquals(listOf(ownId), registry.publications().map { it.id })
        assertEquals(2, auth.get())
        assertEquals(2, calls.get())
    }

    @Test fun `renovacion concurrente comparte token y no consulta red en constructor`() {
        val auth = AtomicInteger()
        tokenEndpoint(auth)
        assertEquals(0, auth.get())
        val pool = Executors.newFixedThreadPool(4)
        try {
            val results = pool.invokeAll((1..12).map { java.util.concurrent.Callable { tokens.token() } }).map { it.get() }
            assertEquals(setOf("token-1"), results.toSet())
            assertEquals(1, auth.get())
        } finally { pool.shutdownNow() }
    }

    @Test fun `403 no renueva y no filtra mensajes del servidor en excepciones`() {
        val auth = AtomicInteger()
        tokenEndpoint(auth)
        server.createContext("/api/v1/publications") { response(it, 403,
            """{"code":"FORBIDDEN","message":"confidential-server-body"}""") }
        val exception = assertFailsWith<CoreHttpException> { registry.publications() }
        assertEquals(403, exception.status)
        assertFalse(exception.toString().contains("confidential-server-body"))
        assertEquals(1, auth.get())
    }

    @Test fun `readiness consulta la raiz y conserva degraded aunque HTTP sea 200`() {
        server.createContext("/health/ready") { request ->
            assertNull(request.requestHeaders.getFirst("Authorization"))
            response(request, 200, """{"status":"degraded","checks":{"database":{"status":"up"},"broker":{"status":"down"}}}""")
        }
        assertEquals("degraded", transport.readiness().get("status").asString())
    }

    @Test fun `sin secret HTTP falla antes de enviar una solicitud`() {
        val requests = AtomicInteger()
        tokenEndpoint(requests)
        properties.machineSecret = ""
        assertEquals("CORE_HTTP_SECRET_MISSING", assertFailsWith<CoreIntegrationException> { tokens.token() }.code)
        assertEquals(0, requests.get())
    }

    @Test fun `dashboard utiliza Bearer cacheado y deserializa metricas camelCase del modulo`() {
        val requests = AtomicInteger()
        tokenEndpoint(requests)
        server.createContext("/api/v1/dashboard") { request ->
            assertEquals("GET", request.requestMethod)
            assertEquals("windowHours=12", request.requestURI.rawQuery)
            assertEquals("Bearer token-1", request.requestHeaders.getFirst("Authorization"))
            response(request, 200, CoreDashboardFixtures.json(windowHours = 12))
        }
        val dashboards = CoreDashboardClient(CoreApiClient(transport, tokens), json, properties)
        val result = dashboards.dashboard(12)
        assertEquals(2L, result.published.inWindow)
        assertEquals(3L, result.received.delivered)
        assertEquals(3.25, result.processing.avgMs)
        assertEquals(2L, result.volumeByHour.single().total)
        dashboards.dashboard(12)
        assertEquals(1, requests.get())
    }

    @Test fun `dashboard rechaza una respuesta perteneciente a otro modulo`() {
        tokenEndpoint(AtomicInteger())
        server.createContext("/api/v1/dashboard") { response(it, 200, CoreDashboardFixtures.json(module = "ciudadanos")) }
        val client = CoreDashboardClient(CoreApiClient(transport, tokens), json, properties)
        assertEquals("CORE_DASHBOARD_IDENTITY_MISMATCH", assertFailsWith<CoreIntegrationException> { client.dashboard(24) }.code)
    }

    @Test fun `listado de eventos codifica filtros una sola vez y no expone campos extra`() {
        tokenEndpoint(AtomicInteger())
        val correlation = UUID.randomUUID()
        server.createContext("/api/v1/events") { request ->
            assertEquals("page=2&size=10&eventType=probe%26other&status=ROUTED&correlationId=$correlation", request.requestURI.rawQuery)
            assertEquals("Bearer token-1", request.requestHeaders.getFirst("Authorization"))
            response(request, 200, """{"items":[],"total":0,"page":2,"size":10,"pages":0,"envelope":"not-in-list-contract"}""")
        }
        val result = CoreEventClient(CoreApiClient(transport, tokens), json).events(2, 10, "probe&other", CoreEventStatus.ROUTED, correlation)
        assertEquals(2, result.page)
        assertTrue(result.items.isEmpty())
        assertFalse(json.writeValueAsString(result).contains("not-in-list-contract"))
    }
}
