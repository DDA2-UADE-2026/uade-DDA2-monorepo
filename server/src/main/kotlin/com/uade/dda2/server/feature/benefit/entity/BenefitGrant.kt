package com.uade.dda2.server.feature.benefit.entity

import com.uade.dda2.server.feature.application.entity.Application
import com.uade.dda2.server.feature.auth.entity.User
import com.uade.dda2.server.feature.program.entity.ProgramBenefit
import com.uade.dda2.server.feature.program.entity.enums.ProgramBenefitType
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "benefit_grant", uniqueConstraints = [
    UniqueConstraint(name = "uk_benefit_grant_application_benefit", columnNames = ["application_id", "program_benefit_id"]),
], indexes = [
    Index(name = "ix_benefit_grant_application_date", columnList = "application_id, assigned_at, id"),
    Index(name = "ix_benefit_grant_benefit", columnList = "program_benefit_id"),
    Index(name = "ix_benefit_grant_assigned_by", columnList = "assigned_by_user_id"),
], check = [
    CheckConstraint(name = "ck_benefit_grant_dates", constraint = "start_date <= end_date"),
    CheckConstraint(name = "ck_benefit_grant_status", constraint = "status = 'ACTIVE'"),
    CheckConstraint(name = "ck_benefit_grant_amount", constraint = "amount is null or amount >= 0"),
    CheckConstraint(name = "ck_benefit_grant_type", constraint =
        "benefit_type in ('TAX_EXEMPTION', 'HOUSING_SUBSIDY', 'FOOD_ASSISTANCE', 'UTILITY_SUBSIDY')"),
])
class BenefitGrant(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false, updatable = false)
    val application: Application,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "program_benefit_id", nullable = false, updatable = false)
    val programBenefit: ProgramBenefit,
    @Enumerated(EnumType.STRING)
    @Column(name = "benefit_type", nullable = false, length = 50, updatable = false)
    val benefitType: ProgramBenefitType,
    @Column(length = 500, updatable = false)
    val description: String?,
    @Column(precision = 19, scale = 2, updatable = false)
    val amount: BigDecimal?,
    @Column(name = "start_date", nullable = false, updatable = false)
    val startDate: LocalDate,
    @Column(name = "end_date", nullable = false, updatable = false)
    val endDate: LocalDate,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    var status: BenefitGrantStatus = BenefitGrantStatus.ACTIVE,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assigned_by_user_id", nullable = false, updatable = false)
    val assignedBy: User,
    @Column(name = "assigned_at", nullable = false, updatable = false)
    val assignedAt: OffsetDateTime,
)
