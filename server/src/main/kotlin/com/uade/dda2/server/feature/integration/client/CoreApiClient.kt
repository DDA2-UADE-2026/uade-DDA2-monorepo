package com.uade.dda2.server.feature.integration.client

import com.uade.dda2.server.feature.integration.error.CoreHttpException
import org.springframework.http.HttpMethod
import tools.jackson.databind.JsonNode

class CoreApiClient(private val transport: CoreHttpTransport, private val tokens: CoreTokenClient) {
    fun request(method: HttpMethod, path: String, body: JsonNode? = null): JsonNode {
        val token = tokens.token()
        return try { transport.request(method, path, body, token) }
        catch (exception: CoreHttpException) {
            if (exception.status != 401) throw exception
            tokens.invalidate(token)
            transport.request(method, path, body, tokens.token())
        }
    }
}
