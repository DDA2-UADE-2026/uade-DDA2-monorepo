package com.uade.dda2.server.feature.integration.service

import com.uade.dda2.server.config.CoreProperties
import com.uade.dda2.server.feature.integration.dto.CoreEnvelope
import com.uade.dda2.server.feature.integration.error.CoreIntegrationException
import com.uade.dda2.server.feature.integration.repository.CoreOutboxRepository
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.util.UUID

@Service
@ConditionalOnProperty(prefix = "app.core", name = ["enabled"], havingValue = "true")
class CoreOutboxService(private val repository: CoreOutboxRepository, private val codec: CoreEnvelopeCodec,
    private val properties: CoreProperties, private val clock: Clock) {
    @Transactional(propagation = Propagation.MANDATORY)
    fun enqueue(envelope: CoreEnvelope): UUID {
        if (envelope.sourceModule != properties.module) throw CoreIntegrationException("CORE_INVALID_SOURCE_MODULE")
        if (envelope.eventId.version() != 4) throw CoreIntegrationException("CORE_EVENT_ID_REQUIRES_UUID_V4")
        val payload = codec.encode(envelope)
        val hash = codec.hash(envelope)
        val inserted = repository.insertPending(envelope.eventId, envelope.eventType, envelope.eventVersion,
            envelope.correlationId, envelope.occurredAt.toInstant(), payload, hash, UUID.randomUUID().toString(), clock.instant())
        if (inserted == 0 && repository.findById(envelope.eventId).orElseThrow().payloadHash != hash)
            throw CoreIntegrationException("CORE_EVENT_ID_CONFLICT")
        return envelope.eventId
    }
}
