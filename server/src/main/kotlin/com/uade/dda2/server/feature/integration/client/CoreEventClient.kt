package com.uade.dda2.server.feature.integration.client

import com.uade.dda2.server.feature.integration.dto.CoreEventDetail
import com.uade.dda2.server.feature.integration.error.CoreHttpException
import org.springframework.http.HttpMethod
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.util.UUID

class CoreEventClient(private val api: CoreApiClient, private val json: JsonMapper) {
    fun event(eventId: UUID): CoreEventDetail? = try {
        json.treeToValue(api.request(HttpMethod.GET, "/events/$eventId"), CoreEventDetail::class.java)
    } catch (exception: CoreHttpException) { if (exception.status == 404) null else throw exception }

    fun envelopeSchema(): JsonNode = api.request(HttpMethod.GET, "/events-meta/envelope-schema")
    fun deliveries(): JsonNode = api.request(HttpMethod.GET, "/deliveries")
    fun deadLetters(): JsonNode = api.request(HttpMethod.GET, "/dlq")
}
