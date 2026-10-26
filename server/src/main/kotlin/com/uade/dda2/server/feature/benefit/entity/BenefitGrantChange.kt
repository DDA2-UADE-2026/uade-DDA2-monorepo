package com.uade.dda2.server.feature.benefit.entity

import com.uade.dda2.server.feature.auth.entity.User
import jakarta.persistence.*
import org.hibernate.annotations.Immutable
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Immutable
@Table(name = "benefit_grant_change", uniqueConstraints = [
    UniqueConstraint(name = "uk_benefit_grant_change_sequence", columnNames = ["grant_id", "sequence_number"]),
], indexes = [
    Index(name = "ix_benefit_grant_change_responsible", columnList = "responsible_user_id"),
], check = [
    CheckConstraint(name = "ck_benefit_grant_change_reason", constraint =
        "(action = 'ASSIGNED' and reason is null) or (action <> 'ASSIGNED' and reason is not null and length(trim(reason)) > 0)"),
    CheckConstraint(name = "ck_benefit_grant_change_transition", constraint =
        "(action = 'ASSIGNED' and sequence_number = 1 and previous_status is null and status = 'ACTIVE' and previous_start_date is null and previous_end_date is null) or " +
        "(sequence_number > 1 and previous_status is not null and previous_start_date is not null and previous_end_date is not null and start_date = previous_start_date and (" +
        "(action = 'RENEWED' and previous_status in ('ACTIVE', 'SUSPENDED') and status = 'ACTIVE' and end_date > previous_end_date) or " +
        "(action = 'SUSPENDED' and previous_status = 'ACTIVE' and status = 'SUSPENDED' and end_date = previous_end_date) or " +
        "(action = 'FINALIZED' and previous_status in ('ACTIVE', 'SUSPENDED') and status = 'FINALIZED' and end_date = previous_end_date) or " +
        "(action = 'EXPIRED' and previous_status in ('ACTIVE', 'SUSPENDED') and status = 'EXPIRED' and end_date = previous_end_date)))"),
    CheckConstraint(name = "ck_benefit_grant_change_responsible", constraint =
        "(action = 'EXPIRED' and responsible_user_id is null) or (action <> 'EXPIRED' and responsible_user_id is not null)"),
    CheckConstraint(name = "ck_benefit_grant_change_dates", constraint = "start_date <= end_date"),
])
class BenefitGrantChange(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "grant_id", nullable = false, updatable = false)
    val grant: BenefitGrant,
    @Column(name = "sequence_number", nullable = false, updatable = false)
    val sequenceNumber: Int,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20, updatable = false)
    val action: BenefitGrantChangeAction,
    @Enumerated(EnumType.STRING) @Column(name = "previous_status", length = 20, updatable = false)
    val previousStatus: BenefitGrantStatus?,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20, updatable = false)
    val status: BenefitGrantStatus,
    @Column(name = "previous_start_date", updatable = false)
    val previousStartDate: LocalDate?,
    @Column(name = "previous_end_date", updatable = false)
    val previousEndDate: LocalDate?,
    @Column(name = "start_date", nullable = false, updatable = false)
    val startDate: LocalDate,
    @Column(name = "end_date", nullable = false, updatable = false)
    val endDate: LocalDate,
    @Column(length = 1000, updatable = false)
    val reason: String?,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsible_user_id", updatable = false)
    val responsible: User?,
    @Column(name = "changed_at", nullable = false, updatable = false)
    val changedAt: OffsetDateTime,
) {
    @OneToMany(mappedBy = "change", cascade = [CascadeType.PERSIST])
    @OrderBy("requirementId ASC")
    val requirementChecks: MutableList<BenefitGrantChangeRequirement> = mutableListOf()
}
