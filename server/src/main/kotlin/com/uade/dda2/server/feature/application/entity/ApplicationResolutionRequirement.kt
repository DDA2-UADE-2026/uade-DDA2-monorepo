package com.uade.dda2.server.feature.application.entity

import com.uade.dda2.server.feature.program.entity.enums.ProgramRequirementType
import jakarta.persistence.*
import org.hibernate.annotations.Immutable
import java.util.UUID

@Entity
@Immutable
@Table(name = "application_resolution_requirement", uniqueConstraints = [
    UniqueConstraint(name = "uk_application_resolution_requirement", columnNames = ["resolution_id", "requirement_id"]),
], check = [
    CheckConstraint(name = "ck_application_resolution_requirement_type", constraint =
        "type in ('MIN_AGE', 'MAX_INCOME', 'RESIDENCY_YEARS', 'HAS_CHILDREN')"),
])
class ApplicationResolutionRequirement(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resolution_id", nullable = false, updatable = false)
    val resolution: ApplicationResolution,
    // A historical identifier, deliberately not a FK to the mutable requirement catalog.
    @Column(name = "requirement_id", nullable = false, updatable = false)
    val requirementId: UUID,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 50, updatable = false)
    val type: ProgramRequirementType,
    @Column(name = "requirement_value", nullable = false, length = 255, updatable = false)
    val value: String,
    @Column(length = 500, updatable = false)
    val description: String?,
    @Column(nullable = false, updatable = false)
    val fulfilled: Boolean,
)
