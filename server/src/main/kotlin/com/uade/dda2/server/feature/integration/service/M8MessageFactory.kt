package com.uade.dda2.server.feature.integration.service

import com.uade.dda2.server.feature.integration.contract.M8EventEnvelope
import com.uade.dda2.server.feature.integration.contract.M8EventType
import com.uade.dda2.server.feature.integration.contract.M8Payload
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Component
class M8MessageFactory(private val clock: Clock) {
    fun <T : M8Payload> create(data: T, correlationId: UUID?, causationId: UUID? = null,
        occurredAt: OffsetDateTime = OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC)): M8EventEnvelope<T> =
        M8EventEnvelope(UUID.randomUUID(), M8EventType.fromPayload(data).wireName, M8EventType.VERSION,
            occurredAt, "desarrollo-social", data, correlationId, causationId)
}
