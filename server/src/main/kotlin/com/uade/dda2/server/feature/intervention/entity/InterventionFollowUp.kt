package com.uade.dda2.server.feature.intervention.entity

import com.uade.dda2.server.feature.auth.entity.User
import jakarta.persistence.*
import org.hibernate.annotations.Immutable
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Immutable
@Table(name = "intervention_follow_up", uniqueConstraints = [
    UniqueConstraint(name = "uk_intervention_follow_up_sequence", columnNames = ["action_id", "sequence_number"]),
], indexes = [Index(name = "ix_intervention_follow_up_author", columnList = "author_user_id")], check = [
    CheckConstraint(name = "ck_intervention_follow_up_sequence", constraint = "sequence_number > 0"),
    CheckConstraint(name = "ck_intervention_follow_up_notes", constraint = "length(trim(notes)) > 0"),
    CheckConstraint(name = "ck_intervention_follow_up_result", constraint =
        "(result is null or length(trim(result)) > 0) and (status <> 'COMPLETED' or result is not null)"),
    CheckConstraint(name = "ck_intervention_follow_up_transition", constraint =
        "previous_status in ('PENDING', 'IN_PROGRESS') and status in ('IN_PROGRESS', 'COMPLETED')"),
])
class InterventionFollowUp(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "action_id", nullable = false, updatable = false)
    val action: InterventionAction,
    @Column(name = "sequence_number", nullable = false, updatable = false)
    val sequenceNumber: Int,
    @Enumerated(EnumType.STRING) @Column(name = "previous_status", nullable = false, length = 20, updatable = false)
    val previousStatus: InterventionActionStatus,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20, updatable = false)
    val status: InterventionActionStatus,
    @Column(name = "occurred_on", nullable = false, updatable = false)
    val occurredOn: LocalDate,
    @Column(nullable = false, length = 2000, updatable = false)
    val notes: String,
    @Column(length = 2000, updatable = false)
    val result: String?,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_user_id", nullable = false, updatable = false)
    val author: User,
    @Column(name = "recorded_at", nullable = false, updatable = false)
    val recordedAt: OffsetDateTime,
)
