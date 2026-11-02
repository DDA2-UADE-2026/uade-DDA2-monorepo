package com.uade.dda2.server.feature.integration.client

import com.uade.dda2.server.config.CoreProperties
import com.uade.dda2.server.feature.integration.dto.CoreTokenResponse
import com.uade.dda2.server.feature.integration.error.CoreIntegrationException
import org.springframework.http.HttpMethod
import tools.jackson.databind.json.JsonMapper
import java.time.Clock
import java.time.Instant

class CoreTokenClient(private val transport: CoreHttpTransport, private val json: JsonMapper,
    private val properties: CoreProperties, private val clock: Clock) {
    private var accessToken: String? = null
    private var renewAt = Instant.MIN

    @Synchronized
    fun token(): String {
        if (properties.machineSecret.isBlank()) throw CoreIntegrationException("CORE_HTTP_SECRET_MISSING")
        accessToken?.takeIf { clock.instant().isBefore(renewAt) }?.let { return it }
        val requestedAt = clock.instant()
        val body = json.createObjectNode().put("module", properties.module).put("secret", properties.machineSecret)
        val responseNode = transport.request(HttpMethod.POST, "/auth/module-token", body)
        val response = try { json.treeToValue(responseNode, CoreTokenResponse::class.java) }
        catch (_: Exception) { throw CoreIntegrationException("CORE_INVALID_TOKEN_RESPONSE") }
        if (response.module != properties.module || response.kind != "module" || response.expiresIn <= 0 || response.accessToken.isBlank())
            throw CoreIntegrationException("CORE_INVALID_TOKEN_RESPONSE")
        accessToken = response.accessToken
        renewAt = requestedAt.plusSeconds(response.expiresIn - minOf(30L, response.expiresIn / 10))
        return response.accessToken
    }

    @Synchronized
    fun invalidate(rejectedToken: String) {
        if (accessToken == rejectedToken) { accessToken = null; renewAt = Instant.MIN }
    }
}
