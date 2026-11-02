package com.uade.dda2.server.feature.integration.client

import com.uade.dda2.server.config.CoreProperties
import com.uade.dda2.server.feature.integration.dto.*
import com.uade.dda2.server.feature.integration.error.CoreHttpException
import org.springframework.http.HttpMethod
import org.springframework.web.util.UriUtils
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.nio.charset.StandardCharsets
import java.util.UUID

class CoreRegistryClient(private val api: CoreApiClient, private val json: JsonMapper, private val properties: CoreProperties) {
    fun subscriptions(): List<CoreSubscription> = api.request(HttpMethod.GET, "/subscriptions").asSequence()
        .map { json.treeToValue(it, CoreSubscription::class.java) }.filter { it.moduleName == properties.module }.toList()

    fun publications(): List<CorePublication> = api.request(HttpMethod.GET, "/publications").asSequence()
        .map { json.treeToValue(it, CorePublication::class.java) }.filter { it.moduleName == properties.module }.toList()

    fun eventMap(): JsonNode = api.request(HttpMethod.GET, "/event-types/map")

    fun eventType(name: String): CoreEventType? = try {
        json.treeToValue(api.request(HttpMethod.GET, "/event-types/" + UriUtils.encodePathSegment(name, StandardCharsets.UTF_8)), CoreEventType::class.java)
    } catch (exception: CoreHttpException) { if (exception.status == 404) null else throw exception }

    // Todas las escrituras son operaciones explícitas; no se invocan desde un initializer.
    fun declareType(name: String, schema: JsonNode): CoreEventType = json.treeToValue(api.request(HttpMethod.POST,
        "/event-types", json.createObjectNode().put("name", name).put("ownerModule", properties.module)
            .put("description", "Prueba técnica coordinada de conectividad de Desarrollo Social").set("jsonSchema", schema)), CoreEventType::class.java)

    fun declarePublication(name: String): CorePublication = json.treeToValue(api.request(HttpMethod.POST,
        "/publications", json.createObjectNode().put("eventType", name)), CorePublication::class.java)

    fun subscribe(name: String, maxAttempts: Int = 4): CoreSubscription {
        require(maxAttempts in 1..10)
        return json.treeToValue(api.request(HttpMethod.POST, "/subscriptions",
            json.createObjectNode().put("eventType", name).put("maxAttempts", maxAttempts)), CoreSubscription::class.java)
    }

    fun setSubscriptionActive(id: UUID, active: Boolean) {
        api.request(HttpMethod.POST, "/subscriptions/$id/toggle?active=$active")
    }
}
