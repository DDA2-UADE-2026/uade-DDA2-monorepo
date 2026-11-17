package com.uade.dda2.server.feature.integration

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import com.uade.dda2.server.config.M1DocumentsConfiguration
import com.uade.dda2.server.config.M1DocumentsProperties
import com.uade.dda2.server.feature.integration.error.M1DocumentException
import com.uade.dda2.server.feature.integration.error.M1DocumentFailure
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.MediaType
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.time.Duration
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.*

class M1DocumentClientTest {
    private val executor = Executors.newCachedThreadPool()
    private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
        this.executor = this@M1DocumentClientTest.executor
        start()
    }
    private val id = UUID.randomUUID()
    private val requests = AtomicInteger()
    private val properties = M1DocumentsProperties().apply {
        enabled = true
        baseUrl = "http://127.0.0.1:${server.address.port}"
        filePathTemplate = "/test-only/documents/{documentId}/file"
        serviceBearerToken = "test-service-token"
        connectTimeout = Duration.ofSeconds(1)
        readTimeout = Duration.ofSeconds(2)
        maxFileBytes = 256
    }
    private val client get() = M1DocumentsConfiguration().m1DocumentClient(properties)
    private var handler: (HttpExchange) -> Unit = { respond(it) }

    init { server.createContext("/") { requests.incrementAndGet(); handler(it) } }
    @AfterEach fun stop() { server.stop(0); executor.shutdownNow() }

    private fun respond(exchange: HttpExchange, status: Int = 200, bytes: ByteArray = "%PDF-local-test".toByteArray(),
        type: String? = "application/pdf", chunked: Boolean = false) {
        type?.let { exchange.responseHeaders.add("Content-Type", it) }
        exchange.sendResponseHeaders(status, if (status == 204 || bytes.isEmpty()) -1 else if (chunked) 0 else bytes.size.toLong())
        exchange.responseBody.use { if (status != 204 && bytes.isNotEmpty()) it.write(bytes) }
        exchange.close()
    }

    @Test fun `consulta el UUID una vez usando solo la credencial de servicio`() {
        handler = {
            assertEquals("GET", it.requestMethod)
            assertEquals("/test-only/documents/$id/file", it.requestURI.toString())
            assertEquals("Bearer test-service-token", it.requestHeaders.getFirst("Authorization"))
            assertNull(it.requestHeaders.getFirst("Cookie"))
            assertEquals("application/octet-stream", it.requestHeaders.getFirst("Accept"))
            it.responseHeaders.add("Content-Disposition", "inline; filename=upstream-name.pdf")
            it.responseHeaders.add("Authorization", "Bearer upstream-secret")
            it.responseHeaders.add("Set-Cookie", "upstream=secret")
            respond(it)
        }
        val result = client.download(id)
        assertContentEquals("%PDF-local-test".toByteArray(), result.bytes)
        assertEquals(MediaType.APPLICATION_PDF, result.contentType)
        assertEquals("documento-$id.pdf", result.filename)
        assertEquals(1, requests.get())
    }

    @ParameterizedTest
    @CsvSource("401,ACCESS_DENIED", "403,ACCESS_DENIED", "404,FILE_NOT_FOUND", "429,UNAVAILABLE", "503,UNAVAILABLE",
        "504,TIMEOUT", "500,UPSTREAM_ERROR", "400,UPSTREAM_ERROR", "204,INVALID_RESPONSE", "206,INVALID_RESPONSE")
    fun `traduce errores sin conservar el cuerpo ni credenciales`(status: Int, failure: M1DocumentFailure) {
        handler = { respond(it, status, "upstream-private-secret".toByteArray(), "application/json") }
        val error = assertFailsWith<M1DocumentException> { client.download(id) }
        assertEquals(failure, error.failure)
        assertFalse(error.toString().contains("secret"))
        assertNull(error.cause)
        assertEquals(1, requests.get())
    }

    @ParameterizedTest
    @ValueSource(ints = [301, 302, 307, 308])
    fun `no sigue redirecciones ni envia credenciales a otro destino`(status: Int) {
        val redirected = AtomicInteger()
        server.createContext("/target") { redirected.incrementAndGet(); respond(it) }
        handler = {
            it.responseHeaders.add("Location", "${properties.baseUrl}/target")
            respond(it, status)
        }
        assertEquals(M1DocumentFailure.INVALID_RESPONSE, assertFailsWith<M1DocumentException> { client.download(id) }.failure)
        assertEquals(1, requests.get())
        assertEquals(0, redirected.get())
    }

    @Test fun `limita cuerpos con longitud declarada y chunked y admite el limite exacto`() {
        for (chunked in listOf(false, true)) {
            handler = { respond(it, bytes = ByteArray(257), chunked = chunked) }
            assertEquals(M1DocumentFailure.TOO_LARGE, assertFailsWith<M1DocumentException> { client.download(id) }.failure)
            handler = { respond(it, bytes = ByteArray(256), chunked = chunked) }
            assertEquals(256, client.download(id).bytes.size)
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["text/html", "application/json", "application/problem+json", "invalid mime"])
    fun `no entrega paginas de login ni errores como documentos`(type: String) {
        handler = { respond(it, type = type) }
        assertEquals(M1DocumentFailure.INVALID_RESPONSE, assertFailsWith<M1DocumentException> { client.download(id) }.failure)
    }

    @Test fun `rechaza cuerpos vacios y codificaciones no soportadas`() {
        handler = { respond(it, bytes = byteArrayOf()) }
        assertEquals(M1DocumentFailure.INVALID_RESPONSE, assertFailsWith<M1DocumentException> { client.download(id) }.failure)
        handler = { it.responseHeaders.add("Content-Encoding", "br"); respond(it) }
        assertEquals(M1DocumentFailure.INVALID_RESPONSE, assertFailsWith<M1DocumentException> { client.download(id) }.failure)
    }

    @Test fun `gzip transparente respeta el limite sobre los bytes descomprimidos`() {
        fun zipped(bytes: ByteArray) = java.io.ByteArrayOutputStream().also { output ->
            java.util.zip.GZIPOutputStream(output).use { it.write(bytes) }
        }.toByteArray()
        val small = "%PDF-local-test".toByteArray()
        handler = { it.responseHeaders.add("Content-Encoding", "gzip"); respond(it, bytes = zipped(small)) }
        assertContentEquals(small, client.download(id).bytes)
        val large = zipped(ByteArray(257))
        assertTrue(large.size < properties.maxFileBytes)
        handler = { it.responseHeaders.add("Content-Encoding", "gzip"); respond(it, bytes = large) }
        assertEquals(M1DocumentFailure.TOO_LARGE, assertFailsWith<M1DocumentException> { client.download(id) }.failure)
    }

    @ParameterizedTest
    @CsvSource("image/png,png", "image/jpeg,jpg", "application/octet-stream,bin", "image/svg+xml,bin")
    fun `nombres controlados y tipos activos como binarios`(type: String, extension: String) {
        handler = { respond(it, type = type) }
        val file = client.download(id)
        assertEquals("documento-$id.$extension", file.filename)
        if (extension == "bin") assertEquals(MediaType.APPLICATION_OCTET_STREAM, file.contentType)
    }

    @Test fun `conexion rechazada se informa sin detalles de transporte`() {
        properties.baseUrl = "http://127.0.0.1:${ServerSocket(0).use { it.localPort }}"
        assertEquals(M1DocumentFailure.UNAVAILABLE, assertFailsWith<M1DocumentException> { client.download(id) }.failure)
    }

    @Test fun `timeout antes de encabezados y durante el cuerpo no devuelve un archivo parcial`() {
        properties.readTimeout = Duration.ofMillis(150)
        handler = { Thread.sleep(500); runCatching { respond(it) } }
        assertEquals(M1DocumentFailure.TIMEOUT, assertFailsWith<M1DocumentException> { client.download(id) }.failure)
        handler = {
            it.responseHeaders.add("Content-Type", "application/pdf")
            it.sendResponseHeaders(200, 20)
            it.responseBody.write(1)
            it.responseBody.flush()
            Thread.sleep(500)
            it.close()
        }
        assertEquals(M1DocumentFailure.TIMEOUT, assertFailsWith<M1DocumentException> { client.download(id) }.failure)
    }
}
