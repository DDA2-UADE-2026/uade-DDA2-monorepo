package com.uade.dda2.server.feature.activity.entity

import com.uade.dda2.server.feature.auth.entity.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Entity
@Table(
    name = "activity_enrollment",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_activity_enrollment_activity_citizen",
            columnNames = ["activity_id", "citizen_id"],
        ),
    ],
    indexes = [
        Index(name = "ix_activity_enrollment_activity", columnList = "activity_id"),
        Index(name = "ix_activity_enrollment_citizen", columnList = "citizen_id"),
    ],
)
class ActivityEnrollment(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "activity_id", nullable = false, updatable = false)
    var activity: Activity,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "citizen_id", nullable = false, updatable = false)
    var citizen: User,

    @Column(name = "enrolled_at", nullable = false, updatable = false)
    var enrolledAt: OffsetDateTime = OffsetDateTime.now(ZoneOffset.UTC),
)
