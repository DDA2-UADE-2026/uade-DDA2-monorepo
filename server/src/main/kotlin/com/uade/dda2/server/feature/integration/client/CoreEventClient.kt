package com.uade.dda2.server.feature.integration.client

import com.uade.dda2.server.feature.integration.dto.CoreEventDetail
import com.uade.dda2.server.feature.integration.dto.CoreEventPage
import com.uade.dda2.server.feature.integration.dto.CoreEventStatus
import com.uade.dda2.server.feature.integration.error.CoreHttpException
import com.uade.dda2.server.feature.integration.error.CoreIntegrationException
import org.springframework.http.HttpMethod
import org.springframework.web.util.UriComponentsBuilder
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.util.UUID

class CoreEventClient(private val api: CoreApiClient, private val json: JsonMapper) {
    fun events(page: Int, size: Int, eventType: String?, status: CoreEventStatus?, correlationId: UUID?): CoreEventPage {
        val path = UriComponentsBuilder.fromPath("/events").queryParam("page", page).queryParam("size", size)
        eventType?.let { path.queryParam("eventType", it) }
        status?.let { path.queryParam("status", it.name) }
        correlationId?.let { path.queryParam("correlationId", it) }
        val node = api.request(HttpMethod.GET, path.build().encode().toUriString())
        return try { json.treeToValue(node, CoreEventPage::class.java) }
        catch (_: Exception) { throw CoreIntegrationException("CORE_INVALID_EVENT_PAGE") }
    }

    fun event(eventId: UUID): CoreEventDetail? = try {
        json.treeToValue(api.request(HttpMethod.GET, "/events/$eventId"), CoreEventDetail::class.java)
    } catch (exception: CoreHttpException) { if (exception.status == 404) null else throw exception }

    fun envelopeSchema(): JsonNode = api.request(HttpMethod.GET, "/events-meta/envelope-schema")
    fun deliveries(): JsonNode = api.request(HttpMethod.GET, "/deliveries")
    fun deadLetters(): JsonNode = api.request(HttpMethod.GET, "/dlq")
}
