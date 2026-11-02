package com.uade.dda2.server.feature.integration.client

import com.uade.dda2.server.feature.integration.error.CoreHttpException
import com.uade.dda2.server.feature.integration.error.CoreIntegrationException
import com.uade.dda2.server.feature.integration.util.CoreFailureClassifier
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.net.URI

class CoreHttpTransport(private val client: RestClient, private val json: JsonMapper, private val baseUrl: String) {
    fun request(method: HttpMethod, path: String, body: JsonNode? = null, token: String? = null): JsonNode {
        return execute(method, baseUrl.trimEnd('/') + path, body, token)
    }

    fun readiness(): JsonNode = execute(HttpMethod.GET, URI.create(baseUrl).resolve("/health/ready").toString(), null, null)

    private fun execute(method: HttpMethod, url: String, body: JsonNode?, token: String?): JsonNode {
        try {
            val request = client.method(method).uri(url)
                .accept(MediaType.APPLICATION_JSON)
            token?.let { request.headers { headers -> headers.setBearerAuth(it) } }
            body?.let { request.contentType(MediaType.APPLICATION_JSON).body(json.writeValueAsString(it)) }
            val response = request.retrieve().toEntity(String::class.java)
            return response.body?.takeIf { it.isNotBlank() }?.let(json::readTree) ?: json.createObjectNode()
        } catch (exception: RestClientResponseException) {
            val error = runCatching { json.readTree(exception.responseBodyAsString) }.getOrNull()
            val code = error?.get("code")?.takeIf { it.isString }?.asString()
                ?.takeIf { it.matches(Regex("[A-Z0-9_]{1,80}")) } ?: "CORE_HTTP_ERROR"
            val trace = exception.responseHeaders?.getFirst("X-Trace-Id")?.take(100)
            throw CoreHttpException(exception.statusCode.value(), code, trace)
        } catch (exception: CoreIntegrationException) {
            throw exception
        } catch (exception: Exception) {
            throw CoreIntegrationException(CoreFailureClassifier.code(exception, "CORE_HTTP_UNAVAILABLE"))
        }
    }
}
