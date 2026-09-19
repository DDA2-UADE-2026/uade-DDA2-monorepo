package com.uade.dda2.server.feature.center.entity

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
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Entity
@Table(
    name = "center_opening_hour",
    indexes = [
        Index(
            name = "ix_center_opening_hour_center_day_active",
            columnList = "center_id, day_of_week, active",
        ),
    ],
    check = [
        CheckConstraint(
            name = "ck_center_opening_hour_time_range",
            constraint = "start_time < end_time",
        ),
    ],
)
class CenterOpeningHour(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "center_id", nullable = false, updatable = false)
    var center: MunicipalCenter,

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 9)
    var dayOfWeek: DayOfWeek,

    @Column(name = "start_time", nullable = false)
    var startTime: LocalTime,

    @Column(name = "end_time", nullable = false)
    var endTime: LocalTime,

    @Column(nullable = false)
    var active: Boolean = true,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now(ZoneOffset.UTC),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now(ZoneOffset.UTC),
) {
    @PrePersist
    fun prePersist() {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        createdAt = now
        updatedAt = now
    }

    @PreUpdate
    fun preUpdate() {
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC)
    }
}
