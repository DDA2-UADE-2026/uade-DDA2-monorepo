package com.uade.dda2.server.feature.benefit.mapper

import com.uade.dda2.server.feature.benefit.dto.response.BenefitGrantChangeResponse
import com.uade.dda2.server.feature.benefit.dto.response.BenefitGrantChangeRequirementResponse
import com.uade.dda2.server.feature.benefit.entity.BenefitGrantChange

fun BenefitGrantChange.toResponse() = BenefitGrantChangeResponse(
    id = requireNotNull(id), grantId = requireNotNull(grant.id), applicationId = requireNotNull(grant.application.id),
    sequenceNumber = sequenceNumber, action = action, previousStatus = previousStatus, status = status,
    previousStartDate = previousStartDate, previousEndDate = previousEndDate, startDate = startDate, endDate = endDate,
    reason = reason, responsibleUserId = requireNotNull(responsible.id), responsibleUserName = responsible.name, changedAt = changedAt,
    requirementChecks = requirementChecks.map { BenefitGrantChangeRequirementResponse(it.requirementId, it.type, it.value, it.description, it.fulfilled) },
)

fun BenefitGrantChange.toAuditSnapshot(): Map<String, Any?> = mapOf(
    "id" to id, "grantId" to grant.id, "applicationId" to grant.application.id, "userId" to grant.application.user.id,
    "sequenceNumber" to sequenceNumber, "action" to action, "previousStatus" to previousStatus, "status" to status,
    "previousStartDate" to previousStartDate, "previousEndDate" to previousEndDate,
    "startDate" to startDate, "endDate" to endDate, "responsibleUserId" to responsible.id, "changedAt" to changedAt,
)
