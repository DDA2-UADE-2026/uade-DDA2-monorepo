package com.uade.dda2.server.feature.benefit.mapper

import com.uade.dda2.server.feature.benefit.dto.response.BenefitGrantResponse
import com.uade.dda2.server.feature.benefit.entity.BenefitGrant

fun BenefitGrant.toResponse() = BenefitGrantResponse(
    id = requireNotNull(id), applicationId = requireNotNull(application.id), userId = requireNotNull(application.user.id),
    programBenefitId = requireNotNull(programBenefit.id), benefitType = benefitType, description = description, amount = amount,
    startDate = startDate, endDate = endDate, status = status, assignedByUserId = requireNotNull(assignedBy.id),
    assignedByUserName = assignedBy.name, assignedAt = assignedAt,
)

fun BenefitGrant.toAuditSnapshot(): Map<String, Any?> = mapOf(
    "id" to id, "applicationId" to application.id, "programBenefitId" to programBenefit.id,
    "assignedByUserId" to assignedBy.id, "status" to status, "startDate" to startDate, "endDate" to endDate, "assignedAt" to assignedAt,
)
