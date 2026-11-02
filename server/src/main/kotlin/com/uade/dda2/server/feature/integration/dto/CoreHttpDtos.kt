package com.uade.dda2.server.feature.integration.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import tools.jackson.databind.JsonNode
import java.util.UUID

// No es data class: toString no debe revelar el accessToken.
@JsonIgnoreProperties(ignoreUnknown = true)
class CoreTokenResponse(
    val accessToken: String,
    val expiresIn: Long,
    val module: String,
    val kind: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CorePublication(val id: UUID, val moduleName: String, val eventType: String, val active: Boolean)
@JsonIgnoreProperties(ignoreUnknown = true)
data class CoreSubscription(val id: UUID, val moduleName: String, val eventType: String,
    val queueName: String, val maxAttempts: Int, val active: Boolean)
@JsonIgnoreProperties(ignoreUnknown = true)
data class CoreEventType(val name: String, val ownerModule: String?, val jsonSchema: JsonNode?,
    val discovered: Boolean = false)
@JsonIgnoreProperties(ignoreUnknown = true)
data class CoreDelivery(val targetModule: String, val queueName: String, val status: String,
    val attempts: Int, val maxAttempts: Int)
@JsonIgnoreProperties(ignoreUnknown = true)
data class CoreEventDetail(val eventId: UUID, val eventType: String, val status: String,
    val ingestionChannel: String, val envelope: JsonNode, val deliveries: List<CoreDelivery> = emptyList())
