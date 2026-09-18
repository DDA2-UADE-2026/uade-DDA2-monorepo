package com.uade.dda2.server.feature.activity.entity

import com.uade.dda2.server.feature.auth.entity.User
import jakarta.persistence.Column
import jakarta.persistence.CheckConstraint
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Entity
@Table(
    name = "activity",
    check = [
        CheckConstraint(name = "ck_activity_positive_capacity", constraint = "capacity > 0"),
        CheckConstraint(name = "ck_activity_date_range", constraint = "start_date <= end_date"),
    ],
)
class Activity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(nullable = false, length = 200)
    var name: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    var description: String,

    @Column(nullable = false, length = 300)
    var location: String,

    @Column(name = "start_date", nullable = false)
    var startDate: LocalDate,

    @Column(name = "end_date", nullable = false)
    var endDate: LocalDate,

    @Column(nullable = false)
    var capacity: Int,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: ActivityStatus = ActivityStatus.DRAFT,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false, updatable = false)
    var createdBy: User,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now(ZoneOffset.UTC),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now(ZoneOffset.UTC),
) {
    @PrePersist
    fun prePersist() {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        trimText()
        createdAt = now
        updatedAt = now
    }

    @PreUpdate
    fun preUpdate() {
        trimText()
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC)
    }

    private fun trimText() {
        name = name.trim()
        description = description.trim()
        location = location.trim()
    }
}
