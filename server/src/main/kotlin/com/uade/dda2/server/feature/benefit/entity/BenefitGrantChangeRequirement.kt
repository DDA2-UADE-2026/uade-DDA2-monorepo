package com.uade.dda2.server.feature.benefit.entity

import com.uade.dda2.server.feature.program.entity.enums.ProgramRequirementType
import jakarta.persistence.*
import org.hibernate.annotations.Immutable
import java.util.UUID

@Entity
@Immutable
@Table(name = "benefit_grant_change_requirement", uniqueConstraints = [
    UniqueConstraint(name = "uk_benefit_grant_change_requirement", columnNames = ["change_id", "requirement_id"]),
], check = [
    CheckConstraint(name = "ck_benefit_grant_change_requirement_type", constraint =
        "type in ('MIN_AGE', 'MAX_INCOME', 'RESIDENCY_YEARS', 'HAS_CHILDREN')"),
])
class BenefitGrantChangeRequirement(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "change_id", nullable = false, updatable = false)
    val change: BenefitGrantChange,
    // Historical identifier: no FK to the editable/deletable requirement catalog.
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
