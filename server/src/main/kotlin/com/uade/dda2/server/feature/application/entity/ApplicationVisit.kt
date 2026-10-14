package com.uade.dda2.server.feature.application.entity

import com.uade.dda2.server.feature.auth.entity.User
import jakarta.persistence.CheckConstraint
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(
    name = "application_visit",
    indexes = [
        Index(name = "ix_application_visit_application", columnList = "application_id"),
        Index(name = "ix_application_visit_application_status", columnList = "application_id, status"),
        Index(
            name = "ix_application_visit_responsible_time",
            columnList = "responsible_user_id, starts_at, ends_at, status",
        ),
        Index(name = "ix_application_visit_result_recorded_by", columnList = "result_recorded_by_user_id"),
        Index(name = "ix_application_visit_starts_at", columnList = "starts_at"),
    ],
    check = [
        CheckConstraint(
            name = "ck_application_visit_type",
            constraint = "type in ('VISIT', 'INTERVIEW')",
        ),
        CheckConstraint(
            name = "ck_application_visit_status",
            constraint = "status in ('SCHEDULED', 'CANCELLED', 'REALIZED', 'NOT_REALIZED')",
        ),
        CheckConstraint(
            name = "ck_application_visit_time_range",
            constraint = "starts_at < ends_at",
        ),
        CheckConstraint(
            name = "ck_application_visit_lifecycle_data",
            constraint = "(status = 'SCHEDULED' and cancellation_reason is null and cancelled_at is null " +
                "and cancelled_by_user_id is null and result_observations is null and result_conclusions is null " +
                "and result_recorded_at is null and result_recorded_by_user_id is null) or " +
                "(status = 'CANCELLED' and cancellation_reason is not null " +
                "and length(trim(cancellation_reason)) between 1 and 1000 and cancelled_at is not null " +
                "and cancelled_by_user_id is not null and result_observations is null and result_conclusions is null " +
                "and result_recorded_at is null and result_recorded_by_user_id is null) or " +
                "(status in ('REALIZED', 'NOT_REALIZED') and cancellation_reason is null and cancelled_at is null " +
                "and cancelled_by_user_id is null and result_recorded_at is not null " +
                "and result_recorded_by_user_id = responsible_user_id and result_recorded_at >= ends_at " +
                "and (result_observations is null or length(trim(result_observations)) between 1 and 2000) " +
                "and (result_conclusions is null or length(trim(result_conclusions)) between 1 and 2000))",
        ),
    ],
)
class ApplicationVisit(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false, updatable = false)
    var application: Application,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    var type: ApplicationVisitType,

    @Column(name = "starts_at", nullable = false)
    var startsAt: OffsetDateTime,

    @Column(name = "ends_at", nullable = false)
    var endsAt: OffsetDateTime,

    @Column(nullable = false, length = 300)
    var location: String,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "responsible_user_id", nullable = false, updatable = false)
    var responsible: User,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: ApplicationVisitStatus = ApplicationVisitStatus.SCHEDULED,

    @Column(name = "scheduled_at", nullable = false, updatable = false)
    var scheduledAt: OffsetDateTime,

    @Column(name = "cancellation_reason", length = 1000)
    var cancellationReason: String? = null,

    @Column(name = "cancelled_at")
    var cancelledAt: OffsetDateTime? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_by_user_id")
    var cancelledBy: User? = null,

    @Column(name = "result_observations", length = 2000)
    var resultObservations: String? = null,

    @Column(name = "result_conclusions", length = 2000)
    var resultConclusions: String? = null,

    @Column(name = "result_recorded_at")
    var resultRecordedAt: OffsetDateTime? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "result_recorded_by_user_id")
    var resultRecordedBy: User? = null,
)
