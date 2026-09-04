package com.uade.dda2.server.feature.application.mapper

import com.uade.dda2.server.feature.application.dto.response.ApplicationResponse
import com.uade.dda2.server.feature.application.entity.Application

fun Application.toResponse() = ApplicationResponse(
    id = requireNotNull(id), applicationNumber = requireNotNull(applicationNumber),
    userId = requireNotNull(user.id), registeredByUserId = requireNotNull(registeredBy.id),
    programEditionId = requireNotNull(programEdition.id), enrollmentPeriodId = requireNotNull(enrollmentPeriod.id),
    status = status, submittedAt = submittedAt, createdAt = createdAt, updatedAt = updatedAt,
)

fun Application.toAuditSnapshot(): Map<String, Any?> = mapOf(
    "id" to id.toString(), "userId" to user.id, "applicationNumber" to applicationNumber,
    "registeredByUserId" to registeredBy.id,
    "programEditionId" to programEdition.id.toString(), "enrollmentPeriodId" to enrollmentPeriod.id.toString(),
    "status" to status.name, "submittedAt" to submittedAt.toString(),
)
