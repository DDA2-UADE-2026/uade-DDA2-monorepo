package com.uade.dda2.server.feature.center.entity

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
    name = "center_service",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_center_service_center_service",
            columnNames = ["center_id", "service_id"],
        ),
    ],
    indexes = [
        Index(name = "ix_center_service_center_active", columnList = "center_id, active"),
        Index(name = "ix_center_service_service", columnList = "service_id"),
    ],
)
class CenterService(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "center_id", nullable = false, updatable = false)
    var center: MunicipalCenter,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false, updatable = false)
    var service: MunicipalService,

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
