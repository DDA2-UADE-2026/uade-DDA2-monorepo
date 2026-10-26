package com.uade.dda2.server.feature.application.mapper

import com.uade.dda2.server.feature.application.dto.response.ApplicationResolutionResponse
import com.uade.dda2.server.feature.application.dto.response.ApplicationResolutionRequirementResponse
import com.uade.dda2.server.feature.application.entity.ApplicationResolution

fun ApplicationResolution.toResponse() = ApplicationResolutionResponse(
    id = requireNotNull(id), applicationId = requireNotNull(application.id), previousStatus = previousStatus,
    decision = decision, reason = reason, resolvedByUserId = requireNotNull(resolvedBy.id), resolvedByUserName = resolvedBy.name,
    resolvedAt = resolvedAt, requirementChecks = requirementChecks.sortedBy { it.requirementId }.map {
        ApplicationResolutionRequirementResponse(it.requirementId, it.type, it.value, it.description, it.fulfilled)
    },
)

fun ApplicationResolution.toAuditSnapshot(): Map<String, Any?> = mapOf(
    "id" to id, "applicationId" to application.id, "previousStatus" to previousStatus,
    "decision" to decision, "resolvedByUserId" to resolvedBy.id, "resolvedAt" to resolvedAt,
    "requirementChecks" to requirementChecks.map { mapOf("requirementId" to it.requirementId, "fulfilled" to it.fulfilled) },
)
