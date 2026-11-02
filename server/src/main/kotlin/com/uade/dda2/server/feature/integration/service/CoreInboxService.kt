package com.uade.dda2.server.feature.integration.service

import com.uade.dda2.server.config.CoreProperties
import com.uade.dda2.server.feature.integration.dto.*
import com.uade.dda2.server.feature.integration.error.CoreIntegrationException
import com.uade.dda2.server.feature.integration.repository.CoreInboxRepository
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

@Service
@ConditionalOnProperty(prefix = "app.core", name = ["enabled"], havingValue = "true")
class CoreInboxService(private val repository: CoreInboxRepository, private val codec: CoreEnvelopeCodec,
    handlers: List<CoreEventHandler>, private val properties: CoreProperties, private val clock: Clock) {
    private val handlersByType = handlers.associateBy { it.eventType to it.eventVersion }.also {
        require(it.size == handlers.size) { "Handlers Core duplicados para el mismo contrato." }
    }

    @Transactional
    fun process(bytes: ByteArray, metadata: CoreMessageMetadata): CoreInboxResult {
        val envelope = codec.decode(bytes)
        if (metadata.messageId?.let { it != envelope.eventId.toString() } == true ||
            metadata.eventIdHeader?.let { it != envelope.eventId.toString() } == true ||
            metadata.targetModule?.let { it != properties.module } == true)
            throw CoreIntegrationException("CORE_TRANSPORT_IDENTITY_MISMATCH")
        val handler = handlersByType[envelope.eventType to envelope.eventVersion]
            ?: throw CoreIntegrationException("CORE_UNSUPPORTED_EVENT")
        val hash = codec.hash(envelope)
        val inserted = repository.insertReceipt(envelope.eventId, envelope.eventType, envelope.eventVersion,
            envelope.sourceModule, hash, envelope.correlationId, envelope.occurredAt.toInstant(), clock.instant(),
            metadata.messageId?.take(100), metadata.traceId?.take(100), metadata.targetModule, metadata.attempt)
        if (inserted == 0) {
            if (repository.findById(envelope.eventId).orElseThrow().payloadHash != hash)
                throw CoreIntegrationException("CORE_EVENT_ID_CONFLICT")
            return CoreInboxResult.DUPLICATE
        }
        handler.handle(envelope)
        return CoreInboxResult.PROCESSED
    }
}
