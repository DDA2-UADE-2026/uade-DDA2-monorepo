package com.uade.dda2.server.feature.enrollmentperiod.entity

import com.uade.dda2.server.feature.program.entity.ProgramEdition
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "enrollment_period",
    indexes = [
        Index(
            name = "ix_enrollment_period_edition_dates",
            columnList = "program_edition_id, open_date, close_date",
        ),
        Index(
            name = "ix_enrollment_period_status_dates",
            columnList = "status, open_date, close_date",
        ),
        Index(
            name = "ix_enrollment_period_edition_status",
            columnList = "program_edition_id, status",
        ),
    ],
    check = [
        CheckConstraint(
            name = "ck_enrollment_period_date_range",
            constraint = "open_date <= close_date",
        ),
    ],
)
class EnrollmentPeriod(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "program_edition_id",
        nullable = false,
    )
    var programEdition: ProgramEdition,

    @Column(
        name = "open_date",
        nullable = false,
    )
    var openDate: LocalDate,

    @Column(
        name = "close_date",
        nullable = false,
    )
    var closeDate: LocalDate,

    @Enumerated(EnumType.STRING)
    @Column(
        nullable = false,
        length = 20,
    )
    var status: EnrollmentPeriodStatus = EnrollmentPeriodStatus.SCHEDULED,

    @Column(length = 1000)
    var notes: String? = null,

    @Column(
        name = "created_at",
        nullable = false,
        updatable = false,
    )
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(
        name = "updated_at",
        nullable = false,
    )
    var updatedAt: LocalDateTime = LocalDateTime.now(),
) {

    @PrePersist
    fun prePersist() {
        val now = LocalDateTime.now()
        notes = notes?.trim()?.ifBlank { null }
        createdAt = now
        updatedAt = now
    }

    @PreUpdate
    fun preUpdate() {
        notes = notes?.trim()?.ifBlank { null }
        updatedAt = LocalDateTime.now()
    }
}
