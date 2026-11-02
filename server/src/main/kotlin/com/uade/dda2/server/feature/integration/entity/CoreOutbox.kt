package com.uade.dda2.server.feature.integration.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

enum class CoreOutboxStatus { PENDING, IN_FLIGHT, BROKER_CONFIRMED }

@Entity
@Table(name = "core_outbox", indexes = [Index(name = "ix_core_outbox_pending", columnList = "status,next_attempt_at")])
class CoreOutbox(
    @Id @Column(name = "event_id") var eventId: UUID = UUID.randomUUID(),
    @Column(name = "event_type", nullable = false, length = 120) var eventType: String = "",
    @Column(name = "event_version", nullable = false, length = 20) var eventVersion: String = "",
    @Column(name = "correlation_id") var correlationId: UUID? = null,
    @Column(name = "occurred_at", nullable = false) var occurredAt: Instant = Instant.EPOCH,
    @Column(name = "payload_json", nullable = false, columnDefinition = "text") var payloadJson: String = "",
    @Column(name = "payload_hash", nullable = false, length = 64) var payloadHash: String = "",
    @Column(name = "trace_id", nullable = false, length = 100) var traceId: String = "",
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) var status: CoreOutboxStatus = CoreOutboxStatus.PENDING,
    @Column(nullable = false) var attempts: Int = 0,
    @Column(name = "next_attempt_at", nullable = false) var nextAttemptAt: Instant = Instant.EPOCH,
    @Column(name = "lease_token") var leaseToken: UUID? = null,
    @Column(name = "lease_until") var leaseUntil: Instant? = null,
    @Column(name = "last_error", length = 100) var lastError: String? = null,
    @Column(name = "created_at", nullable = false) var createdAt: Instant = Instant.EPOCH,
    @Column(name = "confirmed_at") var confirmedAt: Instant? = null,
)
