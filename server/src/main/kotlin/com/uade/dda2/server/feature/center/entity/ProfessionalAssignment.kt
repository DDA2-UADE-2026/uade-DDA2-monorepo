package com.uade.dda2.server.feature.center.entity

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
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Entity
@Table(
    name = "professional_assignment",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_professional_assignment_professional_center_service",
            columnNames = ["professional_id", "center_service_id"],
        ),
    ],
    indexes = [
        Index(name = "ix_professional_assignment_professional_active", columnList = "professional_id, active"),
        Index(name = "ix_professional_assignment_center_service", columnList = "center_service_id"),
    ],
)
class ProfessionalAssignment(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "professional_id", nullable = false, updatable = false)
    var professional: User,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "center_service_id", nullable = false, updatable = false)
    var centerService: CenterService,

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
