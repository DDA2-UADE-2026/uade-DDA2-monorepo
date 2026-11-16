package com.uade.dda2.server.feature.integration.entity

import com.uade.dda2.server.feature.auth.entity.User
import jakarta.persistence.*
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID

enum class SocialTicketState { RECEIVED, IN_PROGRESS, WAITING_INFORMATION, RESOLVED, RETURNED, CLOSED }

@Entity
@Table(name = "social_ticket", indexes = [
    Index(name = "ix_social_ticket_state_updated", columnList = "state,updated_at"),
    Index(name = "ix_social_ticket_application", columnList = "application_id"),
    Index(name = "ix_social_ticket_citizen", columnList = "citizen_id"),
])
class SocialTicket(
    @Id var id: UUID,
    @Column(name = "public_id", nullable = false, length = 120) var publicId: String,
    @Column(name = "citizen_id") var citizenId: UUID? = null,
    @Column(name = "is_anonymous", nullable = false) var anonymous: Boolean = false,
    @Column(name = "responsible_area_id", nullable = false, length = 60) var responsibleAreaId: String = "M8",
    @Column(name = "current_status", nullable = false, length = 60) var currentStatus: String = "ROUTED",
    @Column(name = "current_priority", nullable = false, length = 60) var currentPriority: String = "MEDIUM",
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) var state: SocialTicketState = SocialTicketState.RECEIVED,
    @Column(name = "correlation_id") var correlationId: UUID? = null,
    @Column(name = "last_event_id", nullable = false) var lastEventId: UUID,
    @Column(name = "response_causation_id", nullable = false) var responseCausationId: UUID,
    @Column(name = "response_trigger", nullable = false, length = 60) var responseTrigger: String = "ROUTED",
    @Column(name = "route_event_id", nullable = false) var routeEventId: UUID,
    @Column(name = "started_route_event_id") var startedRouteEventId: UUID? = null,
    @Column(name = "source_updated_at", nullable = false) var sourceUpdatedAt: Instant,
    @Column(name = "snapshot_json", nullable = false, columnDefinition = "text") var snapshotJson: String,
    @Column(name = "application_id") var applicationId: UUID? = null,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "assigned_worker_user_id") var assignedWorker: User? = null,
    @Column(name = "information_pending", nullable = false) var informationPending: Boolean = false,
    @Column(name = "information_required_by") var informationRequiredBy: OffsetDateTime? = null,
    @Column(name = "information_response_json", columnDefinition = "text") var informationResponseJson: String? = null,
    @Column(name = "requires_attention", nullable = false) var requiresAttention: Boolean = true,
    @Column(name = "reopen_reason", length = 2000) var reopenReason: String? = null,
    @Column(name = "closed_reason", length = 1000) var closedReason: String? = null,
    @Column(name = "created_at", nullable = false) var createdAt: Instant,
    @Column(name = "updated_at", nullable = false) var updatedAt: Instant,
    @Column(name = "attachments_json", nullable = false, columnDefinition = "text") var attachmentsJson: String = "[]",
)
