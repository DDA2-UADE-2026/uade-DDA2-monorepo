package com.uade.dda2.server.feature.integration.service

import com.uade.dda2.server.feature.benefit.repository.BenefitGrantRepository
import com.uade.dda2.server.feature.integration.contract.*
import com.uade.dda2.server.feature.integration.entity.RevenueDebt
import com.uade.dda2.server.feature.integration.entity.RevenueExemption
import com.uade.dda2.server.feature.integration.error.CoreIntegrationException
import com.uade.dda2.server.feature.integration.repository.RevenueDebtRepository
import com.uade.dda2.server.feature.integration.repository.RevenueExemptionRepository
import com.uade.dda2.server.feature.log.entity.LogAction
import com.uade.dda2.server.feature.log.entity.LogEntityType
import com.uade.dda2.server.feature.log.service.LogService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(propagation = Propagation.MANDATORY)
class RevenueIncomingService(
    private val debts: RevenueDebtRepository,
    private val exemptions: RevenueExemptionRepository,
    private val grants: BenefitGrantRepository,
    private val logs: LogService,
) {
    fun overdue(message: M8EventEnvelope<DebtOverdueData>) {
        val p = message.data
        val existing = debts.lockById(p.debtId)
        if (existing != null) {
            if (existing.citizenId != p.citizenId) throw CoreIntegrationException("M8_REVENUE_ID_CONFLICT")
            if (existing.settled || message.occurredAt.toInstant().isBefore(existing.sourceUpdatedAt)) return
            existing.outstandingAmount = p.outstandingAmount
            existing.dueDate = p.dueDate
            existing.lastEventId = message.eventId
            existing.sourceUpdatedAt = message.occurredAt.toInstant()
        } else debts.save(RevenueDebt(p.debtId, p.citizenId, p.conceptCode, p.outstandingAmount, p.dueDate,
            lastEventId = message.eventId, sourceUpdatedAt = message.occurredAt.toInstant()))
        logs.record(null, LogAction.UPDATE, LogEntityType.REVENUE_DEBT, p.debtId.toString())
    }

    fun settled(message: M8EventEnvelope<DebtSettledData>) {
        val p = message.data
        if (p.originType != "MUNICIPAL_TAX" || p.citizenId == null) return
        val debt = debts.lockById(p.debtId) ?: return
        if (debt.citizenId != p.citizenId) throw CoreIntegrationException("M8_REVENUE_ID_CONFLICT")
        if (debt.settledAt?.let { p.settledAt.toInstant().isBefore(it) } == true) return
        debt.settled = true
        debt.settledAt = p.settledAt.toInstant()
        debt.remainingBalance = p.remainingBalance
        debt.outstandingBalance = p.outstandingBalance
        debt.lastEventId = message.eventId
        debt.sourceUpdatedAt = message.occurredAt.toInstant()
        logs.record(null, LogAction.UPDATE, LogEntityType.REVENUE_DEBT, p.debtId.toString())
    }

    fun exemptionRequested(message: M8EventEnvelope<ExemptionRequestedData>) {
        val p = message.data
        val row = exemptions.lockById(p.requestId) ?: exemptions.save(RevenueExemption(p.requestId, p.citizenId,
            p.conceptCode, lastEventId = message.eventId))
        if (row.citizenId != p.citizenId || row.conceptCode != p.conceptCode) throw CoreIntegrationException("M8_REVENUE_ID_CONFLICT")
        if (row.requestedEventAt?.let { message.occurredAt.toInstant().isBefore(it) } == true) return
        row.requestReason = p.reason
        row.requestedPercentage = p.requestedPercentage
        row.requestedFrom = p.requestedFrom
        row.requestedUntil = p.requestedUntil
        row.requestedEventAt = message.occurredAt.toInstant()
        row.lastEventId = message.eventId
        val candidates = grants.findActiveTaxExemptions(p.citizenId.toString())
        if (row.benefitGrantId == null) {
            row.benefitGrantId = candidates.singleOrNull()?.id
            row.ambiguousBenefit = candidates.size > 1
        }
        logs.record(null, LogAction.UPDATE, LogEntityType.REVENUE_EXEMPTION, p.requestId.toString())
    }

    fun exemptionUpdated(message: M8EventEnvelope<UpdateExemptionStatusData>) {
        val p = message.data
        val row = exemptions.lockById(p.requestId) ?: exemptions.save(RevenueExemption(p.requestId, p.citizenId,
            p.conceptCode, lastEventId = message.eventId))
        if (row.citizenId != p.citizenId || row.conceptCode != p.conceptCode) throw CoreIntegrationException("M8_REVENUE_ID_CONFLICT")
        if (row.resolutionEventAt?.let { message.occurredAt.toInstant().isBefore(it) } == true) return
        row.status = p.status.name
        row.exemptionId = if (p.status == ExemptionEventStatus.APPROVED) p.exemptionId else null
        row.percentage = if (p.status == ExemptionEventStatus.APPROVED) p.percentage else null
        row.validFrom = if (p.status == ExemptionEventStatus.APPROVED) p.validFrom else null
        row.validUntil = if (p.status == ExemptionEventStatus.APPROVED) p.validUntil else null
        row.resolutionReason = if (p.status == ExemptionEventStatus.REJECTED) p.reason else null
        row.resolutionEventAt = message.occurredAt.toInstant()
        row.lastEventId = message.eventId
        logs.record(null, LogAction.UPDATE, LogEntityType.REVENUE_EXEMPTION, p.requestId.toString())
    }
}
