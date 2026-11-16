package com.uade.dda2.server.feature.integration.service

import com.uade.dda2.server.feature.integration.dto.CoreEnvelope
import com.uade.dda2.server.feature.integration.error.CoreIntegrationException
import org.springframework.stereotype.Component
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.security.MessageDigest
import java.time.OffsetDateTime
import java.util.HexFormat
import java.util.UUID

@Component
class CoreEnvelopeCodec(private val json: JsonMapper) {
    fun decodeIncoming(bytes: ByteArray): CoreEnvelope = try {
        val node = json.readTree(bytes)
        check(node.isObject)
        val canonical = json.createObjectNode()
        FIELDS.forEach { field -> node.get(field)?.let { canonical.set(field, it) } }
        // Los metadatos añadidos por Core no forman parte del sobre lógico ni de su hash.
        decode(json.writeValueAsBytes(canonical))
    } catch (_: Exception) { throw CoreIntegrationException("CORE_INVALID_ENVELOPE") }

    fun decode(bytes: ByteArray): CoreEnvelope = try {
        val node = json.readTree(bytes)
        check(node.isObject && node.properties().all { it.key in FIELDS })
        val data = node.get("data")
        check(data != null && data.isObject)
        CoreEnvelope(
            eventId = uuid(text(node, "eventId")),
            eventType = text(node, "eventType", 120),
            eventVersion = text(node, "eventVersion", 20),
            occurredAt = OffsetDateTime.parse(text(node, "occurredAt")),
            sourceModule = text(node, "sourceModule", 60),
            data = data,
            correlationId = optionalUuid(node, "correlationId"),
            causationId = optionalUuid(node, "causationId"),
        )
    } catch (_: Exception) {
        throw CoreIntegrationException("CORE_INVALID_ENVELOPE")
    }

    fun encode(envelope: CoreEnvelope): String {
        val node = json.createObjectNode()
            .put("eventId", envelope.eventId.toString())
            .put("eventType", envelope.eventType)
            .put("eventVersion", envelope.eventVersion)
            .put("occurredAt", envelope.occurredAt.toString())
            .put("sourceModule", envelope.sourceModule)
        node.set("data", envelope.data)
        envelope.correlationId?.let { node.put("correlationId", it.toString()) }
        envelope.causationId?.let { node.put("causationId", it.toString()) }
        val encoded = json.writeValueAsString(node)
        decode(encoded.toByteArray(Charsets.UTF_8))
        return encoded
    }

    fun hash(envelope: CoreEnvelope): String {
        // Offset equivalente y orden de propiedades no cambian la identidad lógica.
        val normalized = envelope.copy(occurredAt = envelope.occurredAt.toInstant().atOffset(java.time.ZoneOffset.UTC))
        val node = json.readTree(encode(normalized))
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
            .digest(canonical(node).toByteArray(Charsets.UTF_8)))
    }

    private fun canonical(node: JsonNode): String = when {
        node.isObject -> node.properties().sortedBy { it.key }.joinToString(",", "{", "}") {
            json.writeValueAsString(it.key) + ":" + canonical(it.value)
        }
        node.isArray -> node.joinToString(",", "[", "]") { canonical(it) }
        node.isNumber -> node.decimalValue().stripTrailingZeros().toPlainString()
        else -> json.writeValueAsString(node)
    }

    private fun text(node: JsonNode, field: String, max: Int = 200): String {
        val value = node.get(field)
        check(value != null && value.isString)
        return value.asString().also { check(it.isNotBlank() && it == it.trim() && it.length <= max) }
    }

    private fun uuid(value: String): UUID {
        check(UUID_PATTERN.matches(value))
        return UUID.fromString(value)
    }

    private fun optionalUuid(node: JsonNode, field: String): UUID? = node.get(field)
        ?.takeUnless { it.isNull }?.let { uuid(text(node, field)) }

    companion object {
        private val FIELDS = setOf("eventId", "eventType", "eventVersion", "occurredAt", "sourceModule",
            "data", "correlationId", "causationId")
        private val UUID_PATTERN = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
    }
}
