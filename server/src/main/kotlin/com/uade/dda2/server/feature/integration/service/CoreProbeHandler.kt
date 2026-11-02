package com.uade.dda2.server.feature.integration.service

import com.uade.dda2.server.config.CoreProperties
import com.uade.dda2.server.feature.integration.dto.CoreEnvelope
import com.uade.dda2.server.feature.integration.error.CoreIntegrationException
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.util.UUID

class CoreProbeHandler(private val properties: CoreProperties) : CoreEventHandler {
    override val eventType get() = properties.probe.eventType
    override val eventVersion = "1.0"

    override fun handle(envelope: CoreEnvelope) {
        val data = envelope.data
        val run = data.get("runId")?.takeIf { it.isString }?.asString()?.let { runCatching { UUID.fromString(it) }.getOrNull() }
        if (run == null || envelope.sourceModule != properties.module || data.properties().map { it.key }.toSet() != setOf("probe", "runId") ||
            data.get("probe")?.asString() != "core-connectivity" || run != properties.probe.runId || envelope.correlationId != run)
            throw CoreIntegrationException("CORE_INVALID_PROBE")
        // La recepción en inbox es el resultado técnico; no se emite un eco.
    }

    fun schema(json: JsonMapper): JsonNode = json.readTree("""{
        "${'$'}schema":"https://json-schema.org/draft/2020-12/schema","type":"object",
        "properties":{"probe":{"const":"core-connectivity"},"runId":{"type":"string","minLength":1}},
        "required":["probe","runId"],"additionalProperties":false} """)
}
