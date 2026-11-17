package com.uade.dda2.server.feature.integration.client

import com.uade.dda2.server.config.M1AuthProperties
import com.uade.dda2.server.feature.integration.dto.M1CitizenIdentity
import com.uade.dda2.server.feature.integration.dto.M1LoginRequest
import com.uade.dda2.server.feature.integration.error.M1AuthException
import com.uade.dda2.server.feature.integration.error.M1AuthFailure
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.net.URI
import java.net.SocketTimeoutException
import java.net.http.HttpTimeoutException
import java.util.UUID
import java.util.concurrent.TimeUnit

class M1AuthClient(private val client: RestClient, private val json: JsonMapper, properties: M1AuthProperties) {
    private val loginUri = URI.create(properties.baseUrl.trimEnd('/') + "/auth/usuario/login")
    private val reader = json.reader().with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
    private val responseTimeoutNanos = TimeUnit.MILLISECONDS.toNanos(properties.readTimeout.toMillis())

    fun login(credentials: M1LoginRequest): M1CitizenIdentity {
        if (credentials.gmail.isBlank() || credentials.password.isBlank())
            throw M1AuthException(M1AuthFailure.INVALID_REQUEST)
        val startedAt = System.nanoTime()
        try {
            return client.post().uri(loginUri)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(json.writeValueAsString(credentials.copy(gmail = credentials.gmail.trim())))
                .exchange { _, response ->
                    val status = response.statusCode.value()
                    if (status != 200) throw httpFailure(status)
                    val contentType = response.headers.contentType
                    if (contentType == null || contentType.type != "application" ||
                        contentType.subtype != "json" && !contentType.subtype.endsWith("+json")) invalidResponse()
                    // El acuerdo incluye el Bearer en respuesta. Se comprueba su transporte, no su firma.
                    if (!BEARER.matches(response.headers.getFirst(HttpHeaders.AUTHORIZATION).orEmpty())) invalidResponse()
                    val bytes = response.body.readNBytes(MAX_RESPONSE_BYTES + 1)
                    if (bytes.isEmpty() || bytes.size > MAX_RESPONSE_BYTES) invalidResponse()
                    identity(reader.readTree(bytes))
                } ?: invalidResponse()
        } catch (exception: M1AuthException) {
            throw exception
        } catch (exception: ResourceAccessException) {
            val timeout = generateSequence<Throwable>(exception) { it.cause }.take(16)
                .any { it is HttpTimeoutException || it is SocketTimeoutException }
            // Spring cierra el stream al vencer el plazo; durante el cuerpo puede llegar
            // como IOException genérica. El reloj monotónico permite reconocer ese corte.
            val deadlineExceeded = System.nanoTime() - startedAt >= responseTimeoutNanos
            throw M1AuthException(if (timeout || deadlineExceeded) M1AuthFailure.TIMEOUT else M1AuthFailure.UNAVAILABLE)
        } catch (_: RestClientException) {
            throw M1AuthException(M1AuthFailure.INVALID_RESPONSE)
        } catch (_: Exception) {
            // Incluye JSON inválido y errores de lectura: nunca conservar mensajes del proveedor.
            throw M1AuthException(M1AuthFailure.INVALID_RESPONSE)
        }
    }

    private fun identity(root: JsonNode): M1CitizenIdentity {
        if (!root.isObject || root["auth"]?.isBoolean != true) invalidResponse()
        if (!root["auth"].asBoolean()) throw M1AuthException(M1AuthFailure.INVALID_CREDENTIALS, 200)
        val actorType = optionalString(root, "actorType")?.takeIf { it.isNotBlank() } ?: invalidResponse()
        if (actorType != "CIUDADANO") throw M1AuthException(M1AuthFailure.ACTOR_NOT_ALLOWED, 200)
        val citizenId = canonicalUuid(optionalString(root, "citizenId") ?: invalidResponse())
        listOf("actorId", "uuid").forEach { field ->
            optionalString(root, field)?.let { if (canonicalUuid(it) != citizenId) invalidResponse() }
        }
        val id = root["user_id"]?.takeUnless { it.isNull }?.let {
            if (!it.isIntegralNumber || !it.canConvertToLong() || it.asLong() <= 0) invalidResponse()
            it.asLong()
        }
        val name = listOfNotNull(optionalString(root, "nombre"), optionalString(root, "apellido"))
            .map(String::trim).filter(String::isNotEmpty).joinToString(" ")
        if (name.isEmpty() || name.length > 150) invalidResponse()
        return M1CitizenIdentity(citizenId, id, name)
    }

    private fun optionalString(root: JsonNode, field: String): String? {
        val value = root[field]?.takeUnless { it.isNull } ?: return null
        if (!value.isString) invalidResponse()
        return value.asString()
    }

    private fun canonicalUuid(raw: String): UUID {
        if (!UUID_PATTERN.matches(raw)) invalidResponse()
        return UUID.fromString(raw)
    }

    private fun httpFailure(status: Int): M1AuthException = M1AuthException(when (status) {
        401 -> M1AuthFailure.INVALID_CREDENTIALS
        403 -> M1AuthFailure.ACCOUNT_FORBIDDEN
        429 -> M1AuthFailure.RATE_LIMITED
        503 -> M1AuthFailure.UNAVAILABLE
        in 200..399 -> M1AuthFailure.INVALID_RESPONSE
        else -> M1AuthFailure.UPSTREAM_ERROR
    }, status)

    private fun invalidResponse(): Nothing = throw M1AuthException(M1AuthFailure.INVALID_RESPONSE, 200)

    private companion object {
        const val MAX_RESPONSE_BYTES = 65_536
        val BEARER = Regex("Bearer +\\S+", RegexOption.IGNORE_CASE)
        val UUID_PATTERN = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
    }
}
