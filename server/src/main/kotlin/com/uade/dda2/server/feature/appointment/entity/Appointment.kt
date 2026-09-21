package com.uade.dda2.server.feature.appointment.entity

import com.uade.dda2.server.feature.auth.entity.User
import com.uade.dda2.server.feature.center.entity.ProfessionalAssignment
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
import jakarta.persistence.UniqueConstraint
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Entity
@Table(
    name = "appointment",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_appointment_citizen_idempotency",
            columnNames = ["citizen_id", "idempotency_key"],
        ),
        UniqueConstraint(
            name = "uk_appointment_assignment_start",
            columnNames = ["professional_assignment_id", "starts_at"],
        ),
    ],
    indexes = [
        Index(name = "ix_appointment_citizen_time", columnList = "citizen_id, starts_at, ends_at, status"),
        Index(
            name = "ix_appointment_assignment_time",
            columnList = "professional_assignment_id, starts_at, ends_at, status",
        ),
    ],
    check = [
        CheckConstraint(
            name = "ck_appointment_time_range",
            constraint = "starts_at < ends_at",
        ),
    ],
)
class Appointment(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "citizen_id", nullable = false, updatable = false)
    var citizen: User,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "professional_assignment_id", nullable = false, updatable = false)
    var professionalAssignment: ProfessionalAssignment,

    @Column(name = "starts_at", nullable = false, updatable = false)
    var startsAt: OffsetDateTime,

    @Column(name = "ends_at", nullable = false, updatable = false)
    var endsAt: OffsetDateTime,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    var status: AppointmentStatus = AppointmentStatus.CONFIRMED,

    @Column(name = "idempotency_key", nullable = false, length = 128, updatable = false)
    var idempotencyKey: String,

    @Column(name = "request_hash", nullable = false, length = 64, updatable = false)
    var requestHash: String,

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
