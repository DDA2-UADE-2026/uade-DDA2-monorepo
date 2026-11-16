package com.uade.dda2.server.feature.integration.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "social_revenue_debt", indexes = [Index(name = "ix_social_debt_citizen", columnList = "citizen_id")])
class RevenueDebt(
    @Id var id: UUID,
    @Column(name = "citizen_id", nullable = false) var citizenId: UUID,
    @Column(name = "concept_code", nullable = false, length = 120) var conceptCode: String,
    @Column(name = "outstanding_amount", nullable = false, precision = 19, scale = 2) var outstandingAmount: BigDecimal,
    @Column(name = "due_date", nullable = false) var dueDate: LocalDate,
    @Column(nullable = false) var settled: Boolean = false,
    @Column(name = "settled_at") var settledAt: Instant? = null,
    @Column(name = "remaining_balance", precision = 19, scale = 2) var remainingBalance: BigDecimal? = null,
    @Column(name = "outstanding_balance", precision = 19, scale = 2) var outstandingBalance: BigDecimal? = null,
    @Column(name = "last_event_id", nullable = false) var lastEventId: UUID,
    @Column(name = "source_updated_at", nullable = false) var sourceUpdatedAt: Instant,
)

@Entity
@Table(name = "social_exemption_request", indexes = [Index(name = "ix_social_exemption_citizen", columnList = "citizen_id")])
class RevenueExemption(
    @Id var id: UUID,
    @Column(name = "citizen_id", nullable = false) var citizenId: UUID,
    @Column(name = "concept_code", nullable = false, length = 120) var conceptCode: String,
    @Column(name = "request_reason", length = 2000) var requestReason: String? = null,
    @Column(name = "requested_percentage", precision = 7, scale = 2) var requestedPercentage: BigDecimal? = null,
    @Column(name = "requested_from") var requestedFrom: LocalDate? = null,
    @Column(name = "requested_until") var requestedUntil: LocalDate? = null,
    @Column(name = "benefit_grant_id") var benefitGrantId: UUID? = null,
    @Column(name = "ambiguous_benefit", nullable = false) var ambiguousBenefit: Boolean = false,
    @Column(nullable = false, length = 30) var status: String = "REQUESTED",
    @Column(name = "exemption_id") var exemptionId: UUID? = null,
    @Column(precision = 7, scale = 2) var percentage: BigDecimal? = null,
    @Column(name = "valid_from") var validFrom: LocalDate? = null,
    @Column(name = "valid_until") var validUntil: LocalDate? = null,
    @Column(name = "resolution_reason", length = 2000) var resolutionReason: String? = null,
    @Column(name = "last_event_id", nullable = false) var lastEventId: UUID,
    @Column(name = "requested_event_at") var requestedEventAt: Instant? = null,
    @Column(name = "resolution_event_at") var resolutionEventAt: Instant? = null,
)
