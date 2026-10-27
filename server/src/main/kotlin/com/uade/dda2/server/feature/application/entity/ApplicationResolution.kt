package com.uade.dda2.server.feature.application.entity

import com.uade.dda2.server.feature.auth.entity.User
import jakarta.persistence.*
import org.hibernate.annotations.Immutable
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Immutable
@Table(name = "application_resolution", indexes = [
    Index(name = "ix_application_resolution_application", columnList = "application_id, resolved_at"),
    Index(name = "ix_application_resolution_responsible", columnList = "resolved_by_user_id"),
], uniqueConstraints = [
    UniqueConstraint(name = "uk_application_resolution_stage", columnNames = ["application_id", "previous_status"]),
], check = [
    CheckConstraint(name = "ck_application_resolution_transition", constraint =
        "(previous_status = 'IN_EVALUATION' and decision in ('APPROVED', 'REJECTED', 'WAITLISTED')) or " +
            "(previous_status = 'WAITLISTED' and decision in ('APPROVED', 'REJECTED'))"),
    CheckConstraint(name = "ck_application_resolution_reason", constraint = "length(trim(reason)) > 0"),
])
class ApplicationResolution(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false, updatable = false)
    val application: Application,
    @Enumerated(EnumType.STRING) @Column(name = "previous_status", nullable = false, length = 30, updatable = false)
    val previousStatus: ApplicationStatus,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30, updatable = false)
    val decision: ApplicationResolutionDecision,
    @Column(nullable = false, length = 1000, updatable = false)
    val reason: String,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resolved_by_user_id", nullable = false, updatable = false)
    val resolvedBy: User,
    @Column(name = "resolved_at", nullable = false, updatable = false)
    val resolvedAt: OffsetDateTime,
) {
    @OneToMany(mappedBy = "resolution", cascade = [CascadeType.PERSIST])
    @OrderBy("requirementId ASC")
    val requirementChecks: MutableList<ApplicationResolutionRequirement> = mutableListOf()
}
