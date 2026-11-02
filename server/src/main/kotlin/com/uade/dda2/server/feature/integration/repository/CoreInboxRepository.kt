package com.uade.dda2.server.feature.integration.repository

import com.uade.dda2.server.feature.integration.entity.CoreInbox
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.Instant
import java.util.UUID

interface CoreInboxRepository : JpaRepository<CoreInbox, UUID> {
    @Modifying
    @Query(value = """INSERT INTO core_inbox (event_id,event_type,event_version,source_module,payload_hash,
        correlation_id,occurred_at,received_at,processed_at,message_id,trace_id,target_module,delivery_attempt)
        VALUES (:eventId,:eventType,:eventVersion,:sourceModule,:payloadHash,:correlationId,:occurredAt,:now,:now,
        :messageId,:traceId,:targetModule,:attempt) ON CONFLICT DO NOTHING""", nativeQuery = true)
    fun insertReceipt(eventId: UUID, eventType: String, eventVersion: String, sourceModule: String, payloadHash: String,
        correlationId: UUID?, occurredAt: Instant, now: Instant, messageId: String?, traceId: String?,
        targetModule: String?, attempt: Int?): Int
}
