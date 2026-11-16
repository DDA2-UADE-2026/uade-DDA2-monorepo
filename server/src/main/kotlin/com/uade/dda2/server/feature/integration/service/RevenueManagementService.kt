package com.uade.dda2.server.feature.integration.service

import com.uade.dda2.server.error.ConflictException
import com.uade.dda2.server.error.NotFoundException
import com.uade.dda2.server.feature.benefit.entity.BenefitGrantStatus
import com.uade.dda2.server.feature.benefit.repository.BenefitGrantRepository
import com.uade.dda2.server.feature.integration.dto.LinkExemptionBenefitRequest
import com.uade.dda2.server.feature.integration.dto.RevenueDebtResponse
import com.uade.dda2.server.feature.integration.dto.RevenueExemptionResponse
import com.uade.dda2.server.feature.integration.dto.SocialIntegrationPage
import com.uade.dda2.server.feature.integration.entity.RevenueExemption
import com.uade.dda2.server.feature.integration.repository.RevenueDebtRepository
import com.uade.dda2.server.feature.integration.repository.RevenueExemptionRepository
import com.uade.dda2.server.feature.log.entity.LogAction
import com.uade.dda2.server.feature.log.entity.LogEntityType
import com.uade.dda2.server.feature.log.service.LogService
import com.uade.dda2.server.feature.program.entity.enums.ProgramBenefitType
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.json.JsonMapper
import java.util.UUID

@Service
class RevenueManagementService(
    private val debts: RevenueDebtRepository,
    private val exemptions: RevenueExemptionRepository,
    private val grants: BenefitGrantRepository,
    private val access: SocialIntegrationAccess,
    private val policy: M8BusinessPolicy,
    private val logs: LogService,
    private val json: JsonMapper,
) {
    @Transactional(readOnly = true)
    fun debts(citizenId: UUID?, page: Int, size: Int): SocialIntegrationPage<RevenueDebtResponse> {
        access.check("revenue:management:view")
        val pageable = PageRequest.of(page, size, Sort.by("id"))
        val result = if (citizenId == null) debts.findAll(pageable) else debts.findAllByCitizenId(citizenId, pageable)
        return SocialIntegrationPage(result.content.map { RevenueDebtResponse(it.id, it.citizenId, it.conceptCode,
            it.outstandingAmount, it.dueDate, it.settled, it.settledAt, it.remainingBalance, it.outstandingBalance, it.sourceUpdatedAt) },
            result.number, result.size, result.totalElements, result.totalPages)
    }

    @Transactional(readOnly = true)
    fun exemptions(citizenId: UUID?, page: Int, size: Int): SocialIntegrationPage<RevenueExemptionResponse> {
        access.check("revenue:management:view")
        val pageable = PageRequest.of(page, size, Sort.by("id"))
        val result = if (citizenId == null) exemptions.findAll(pageable) else exemptions.findAllByCitizenId(citizenId, pageable)
        return SocialIntegrationPage(result.content.map { response(it) }, result.number, result.size, result.totalElements, result.totalPages)
    }

    @Transactional
    fun link(id: UUID, request: LinkExemptionBenefitRequest): RevenueExemptionResponse {
        val actor = access.check("revenue:management:manage")
        val row = exemptions.lockById(id) ?: throw NotFoundException("M8_EXEMPTION_NOT_FOUND", "No se encontró la solicitud de exención.")
        val grant = grants.findById(request.benefitGrantId).orElse(null)
            ?: throw NotFoundException("BENEFIT_GRANT_NOT_FOUND", "No se encontró el beneficio.")
        if (grant.status != BenefitGrantStatus.ACTIVE || grant.benefitType != ProgramBenefitType.TAX_EXEMPTION ||
            policy.citizenId(grant.application.user) != row.citizenId)
            throw ConflictException("M8_EXEMPTION_BENEFIT_MISMATCH", "La exención debe vincularse a un beneficio TAX_EXEMPTION activo del mismo ciudadano.")
        row.benefitGrantId = requireNotNull(grant.id)
        row.ambiguousBenefit = false
        logs.record(actor, LogAction.UPDATE, LogEntityType.REVENUE_EXEMPTION, id.toString(),
            newValues = json.writeValueAsString(mapOf("benefitGrantId" to grant.id)))
        return response(row)
    }

    private fun response(row: RevenueExemption) = RevenueExemptionResponse(row.id, row.citizenId, row.conceptCode,
        row.requestReason, row.requestedPercentage, row.requestedFrom, row.requestedUntil, row.benefitGrantId,
        row.ambiguousBenefit, row.status, row.exemptionId, row.percentage, row.validFrom, row.validUntil, row.resolutionReason)
}
