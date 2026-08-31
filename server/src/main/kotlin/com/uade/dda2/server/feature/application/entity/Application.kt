package com.uade.dda2.server.feature.application.entity

import com.uade.dda2.server.feature.auth.entity.User
import com.uade.dda2.server.feature.enrollmentperiod.entity.EnrollmentPeriod
import com.uade.dda2.server.feature.program.entity.ProgramEdition
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "application", uniqueConstraints = [
    UniqueConstraint(name = "uk_application_user_period", columnNames = ["user_id", "enrollment_period_id"]),
    UniqueConstraint(name = "uk_application_user_idempotency", columnNames = ["user_id", "idempotency_key"]),
], indexes = [
    Index(name = "ix_application_user_number", columnList = "user_id, application_number"),
    Index(name = "ix_application_user_edition_status", columnList = "user_id, program_edition_id, status"),
    Index(name = "ix_application_period", columnList = "enrollment_period_id"),
    Index(name = "ix_application_edition", columnList = "program_edition_id"),
    Index(name = "ix_application_worker", columnList = "assigned_worker_user_id"),
], check = [CheckConstraint(name = "ck_application_idempotency", constraint =
    "(idempotency_key is null and request_hash is null) or (idempotency_key is not null and request_hash is not null)")])
class Application(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @field:GeneratedApplicationNumber
    @Column(name = "application_number", nullable = false, unique = true, updatable = false)
    var applicationNumber: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    var user: User,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "program_edition_id", nullable = false, updatable = false)
    var programEdition: ProgramEdition,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enrollment_period_id", nullable = false, updatable = false)
    var enrollmentPeriod: EnrollmentPeriod,

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    var status: ApplicationStatus = ApplicationStatus.SUBMITTED,

    @Column(name = "origin_ticket_id", length = 255)
    var originTicketId: String? = null,
    @Column(name = "resolution_reason", length = 1000)
    var resolutionReason: String? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_worker_user_id")
    var assignedWorker: User? = null,
    @Column(name = "submitted_at", updatable = false)
    var submittedAt: LocalDateTime,
    @Column(name = "resolved_at")
    var resolvedAt: LocalDateTime? = null,
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = submittedAt,
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = submittedAt,

    @Column(name = "idempotency_key", length = 128, updatable = false)
    var idempotencyKey: String? = null,
    @Column(name = "request_hash", length = 64, updatable = false)
    var requestHash: String? = null,
)
