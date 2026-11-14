package com.uade.dda2.server.feature.integration.service

import com.uade.dda2.server.feature.integration.contract.*
import com.uade.dda2.server.feature.integration.dto.CoreEnvelope
import com.uade.dda2.server.feature.integration.error.CoreIntegrationException
import com.uade.dda2.server.feature.integration.validator.M8PayloadValidator
import org.springframework.stereotype.Component
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.primaryConstructor

@Component
class M8ContractCodec(private val json: JsonMapper, private val validator: M8PayloadValidator) {
    fun decode(envelope: CoreEnvelope): M8Payload {
        val type = M8EventType.fromWireName(envelope.eventType) ?: throw CoreIntegrationException("M8_UNSUPPORTED_EVENT")
        if (envelope.eventVersion != M8EventType.VERSION || envelope.sourceModule != type.sourceModule)
            throw CoreIntegrationException("M8_INVALID_EVENT_CONTRACT")
        return try {
            checkObject(envelope.data, type.payloadClass.kotlin)
            json.treeToValue(envelope.data, type.payloadClass).also {
                validator.validate(it)
                validateCorrelation(envelope, it)
            }
        } catch (exception: CoreIntegrationException) { throw exception }
        catch (_: Exception) { throw CoreIntegrationException("M8_INVALID_PAYLOAD") }
    }

    fun <T : M8Payload> typed(envelope: CoreEnvelope, payloadClass: Class<T>): M8EventEnvelope<T> {
        val data = decode(envelope)
        if (!payloadClass.isInstance(data)) throw CoreIntegrationException("M8_INVALID_EVENT_CONTRACT")
        return M8EventEnvelope(envelope.eventId, envelope.eventType, envelope.eventVersion, envelope.occurredAt,
            envelope.sourceModule, payloadClass.cast(data), envelope.correlationId, envelope.causationId)
    }

    fun envelope(message: M8EventEnvelope<out M8Payload>): CoreEnvelope {
        val result = CoreEnvelope(message.eventId, message.eventType, message.eventVersion, message.occurredAt,
            message.sourceModule, json.valueToTree(message.data), message.correlationId, message.causationId)
        decode(result)
        return result
    }

    private fun validateCorrelation(envelope: CoreEnvelope, payload: M8Payload) {
        val expected = when (payload) {
            is SocialDocumentationRequestedData -> payload.applicationId
            is SocialBenefitUpdatedData -> payload.applicationId
            is SocialDocumentationDeliveredData -> payload.applicationId
            else -> null
        }
        if (expected != null && envelope.correlationId != expected) throw CoreIntegrationException("M8_INVALID_CORRELATION")
        if (payload is SocialDocumentationDeliveredData && envelope.causationId == null)
            throw CoreIntegrationException("M8_CAUSATION_REQUIRED")
    }

    // El mismo constructor Kotlin distingue obligatorio-nullable de opcional con valor por defecto.
    private fun checkObject(node: JsonNode, type: KClass<*>) {
        check(node.isObject)
        val constructor = type.primaryConstructor ?: return
        constructor.parameters.forEach { parameter ->
            val name = requireNotNull(parameter.name)
            if (!parameter.isOptional) check(node.has(name))
            node.get(name)?.let { checkValue(it, parameter.type) }
        }
    }

    private fun checkValue(node: JsonNode, type: KType) {
        if (node.isNull) { check(type.isMarkedNullable); return }
        when (val klass = type.classifier as? KClass<*> ?: return) {
            String::class -> check(node.isString)
            Boolean::class -> check(node.isBoolean)
            Int::class, Long::class -> check(node.isIntegralNumber)
            BigDecimal::class -> check(node.isNumber)
            UUID::class -> { check(node.isString && UUID_REGEX.matches(node.asString())); UUID.fromString(node.asString()) }
            LocalDate::class -> { check(node.isString); LocalDate.parse(node.asString()) }
            OffsetDateTime::class -> { check(node.isString); OffsetDateTime.parse(node.asString()) }
            List::class -> { check(node.isArray); type.arguments.firstOrNull()?.type?.let { element -> node.forEach { checkValue(it, element) } } }
            else -> when {
                klass.java.isEnum -> check(node.isString && klass.java.enumConstants.any { (it as Enum<*>).name == node.asString() })
                !JsonNode::class.java.isAssignableFrom(klass.java) -> checkObject(node, klass)
            }
        }
    }

    companion object {
        private val UUID_REGEX = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
    }
}
