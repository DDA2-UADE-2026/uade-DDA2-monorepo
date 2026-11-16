package com.uade.dda2.server.feature.integration.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "core_inbox")
class CoreInbox(
    @Id @Column(name = "event_id") var eventId: UUID = UUID.randomUUID(),
    @Column(name = "event_type", nullable = false, length = 120) var eventType: String = "",
    @Column(name = "event_version", nullable = false, length = 20) var eventVersion: String = "",
    @Column(name = "source_module", nullable = false, length = 60) var sourceModule: String = "",
    @Column(name = "payload_hash", nullable = false, length = 64) var payloadHash: String = "",
    @Column(name = "correlation_id") var correlationId: UUID? = null,
    @Column(name = "occurred_at", nullable = false) var occurredAt: Instant = Instant.EPOCH,
    @Column(name = "received_at", nullable = false) var receivedAt: Instant = Instant.EPOCH,
    @Column(name = "processed_at", nullable = false) var processedAt: Instant = Instant.EPOCH,
    @Column(name = "message_id", length = 100) var messageId: String? = null,
    @Column(name = "trace_id", length = 100) var traceId: String? = null,
    @Column(name = "target_module", length = 60) var targetModule: String? = null,
    @Column(name = "delivery_attempt") var deliveryAttempt: Int? = null,
)
