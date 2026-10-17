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
    name = "application_visit_reschedule_request",
    indexes = [
        Index(name = "ix_visit_reschedule_request_visit_time", columnList = "visit_id, requested_at"),
        Index(name = "ix_visit_reschedule_request_status_time", columnList = "status, requested_at"),
        Index(name = "ix_visit_reschedule_request_requested_by", columnList = "requested_by_user_id"),
        Index(name = "ix_visit_reschedule_request_reviewed_by", columnList = "reviewed_by_user_id"),
    ],
    check = [
        CheckConstraint(
            name = "ck_visit_reschedule_request_status",
            constraint = "status in ('PENDING', 'APPROVED', 'REJECTED')",
        ),
        CheckConstraint(
            name = "ck_visit_reschedule_request_reason",
            constraint = "length(trim(reason)) between 1 and 1000",
        ),
        CheckConstraint(
            name = "ck_visit_reschedule_request_original_schedule",
            constraint = "original_starts_at < original_ends_at and " +
                "length(trim(original_location)) between 1 and 300",
        ),
        CheckConstraint(
            name = "ck_visit_reschedule_request_lifecycle",
            constraint = "(status = 'PENDING' and reviewed_by_user_id is null and reviewed_at is null " +
                "and review_comment is null and approved_starts_at is null and approved_ends_at is null " +
                "and approved_location is null) or " +
                "(status = 'APPROVED' and reviewed_by_user_id is not null and reviewed_at is not null " +
                "and reviewed_at >= requested_at and approved_starts_at is not null and approved_ends_at is not null " +
                "and approved_starts_at < approved_ends_at and approved_location is not null " +
                "and length(trim(approved_location)) between 1 and 300 " +
                "and (review_comment is null or length(trim(review_comment)) between 1 and 1000)) or " +
                "(status = 'REJECTED' and reviewed_by_user_id is not null and reviewed_at is not null " +
                "and reviewed_at >= requested_at and review_comment is not null " +
                "and length(trim(review_comment)) between 1 and 1000 and approved_starts_at is null " +
                "and approved_ends_at is null and approved_location is null)",
        ),
    ],
)
class ApplicationVisitRescheduleRequest(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visit_id", nullable = false, updatable = false)
    var visit: ApplicationVisit,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by_user_id", nullable = false, updatable = false)
    var requestedBy: User,

    @Column(nullable = false, length = 1000, updatable = false)
    var reason: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: ApplicationVisitRescheduleRequestStatus = ApplicationVisitRescheduleRequestStatus.PENDING,

    @Column(name = "requested_at", nullable = false, updatable = false)
    var requestedAt: OffsetDateTime,

    @Column(name = "original_starts_at", nullable = false, updatable = false)
    var originalStartsAt: OffsetDateTime,

    @Column(name = "original_ends_at", nullable = false, updatable = false)
    var originalEndsAt: OffsetDateTime,

    @Column(name = "original_location", nullable = false, length = 300, updatable = false)
    var originalLocation: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_user_id")
    var reviewedBy: User? = null,

    @Column(name = "reviewed_at")
    var reviewedAt: OffsetDateTime? = null,

    @Column(name = "review_comment", length = 1000)
    var reviewComment: String? = null,

    @Column(name = "approved_starts_at")
    var approvedStartsAt: OffsetDateTime? = null,

    @Column(name = "approved_ends_at")
    var approvedEndsAt: OffsetDateTime? = null,

    @Column(name = "approved_location", length = 300)
    var approvedLocation: String? = null,
)
