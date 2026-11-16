package com.uade.dda2.server.feature.integration.dto

import tools.jackson.databind.JsonNode
import java.time.OffsetDateTime
import java.util.UUID

data class CoreEnvelope(
    val eventId: UUID,
    val eventType: String,
    val eventVersion: String,
    val occurredAt: OffsetDateTime,
    val sourceModule: String,
    val data: JsonNode,
    val correlationId: UUID? = null,
    val causationId: UUID? = null,
)
