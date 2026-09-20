package com.uade.dda2.server.feature.center.entity

import jakarta.persistence.CheckConstraint
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Entity
@Table(
    name = "municipal_service",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_municipal_service_normalized_name",
            columnNames = ["normalized_name"],
        ),
    ],
    indexes = [
        Index(name = "ix_municipal_service_active", columnList = "active"),
    ],
    check = [
        CheckConstraint(
            name = "ck_municipal_service_positive_duration",
            constraint = "duration_minutes > 0",
        ),
    ],
)
class MunicipalService(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(nullable = false, length = 150)
    var name: String,

    @Column(name = "normalized_name", nullable = false, length = 150)
    var normalizedName: String = MunicipalNameNormalizer.normalize(name),

    @Column(nullable = false, columnDefinition = "TEXT")
    var description: String,

    @Column(name = "duration_minutes", nullable = false)
    var durationMinutes: Int,

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
        normalizeFields()
        createdAt = now
        updatedAt = now
    }

    @PreUpdate
    fun preUpdate() {
        normalizeFields()
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC)
    }

    private fun normalizeFields() {
        name = name.trim()
        normalizedName = MunicipalNameNormalizer.normalize(name)
        description = description.trim()
    }
}
