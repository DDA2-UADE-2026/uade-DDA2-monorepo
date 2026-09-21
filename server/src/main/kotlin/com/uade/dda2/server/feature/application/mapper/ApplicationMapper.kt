package com.uade.dda2.server.feature.application.mapper

import com.uade.dda2.server.feature.application.dto.response.AdminApplicationListItemResponse
import com.uade.dda2.server.feature.application.dto.response.AdminApplicationResponse
import com.uade.dda2.server.feature.application.dto.response.ApplicationResponse
import com.uade.dda2.server.feature.application.entity.Application
import com.uade.dda2.server.feature.program.dto.available.response.AvailableProgramDocumentRequirementResponse

fun Application.toResponse(
    pendingDocuments: List<com.uade.dda2.server.feature.application.dto.response.PendingApplicationDocumentResponse> = emptyList(),
    documentRequirements: List<AvailableProgramDocumentRequirementResponse> = emptyList(),
) = ApplicationResponse(
    id = requireNotNull(id), applicationNumber = requireNotNull(applicationNumber),
    userId = requireNotNull(user.id), registeredByUserId = requireNotNull(registeredBy.id),
    programEditionId = requireNotNull(programEdition.id), enrollmentPeriodId = requireNotNull(enrollmentPeriod.id),
    status = status, submittedAt = submittedAt, createdAt = createdAt, updatedAt = updatedAt,
    pendingDocuments = pendingDocuments,
    programId = requireNotNull(programEdition.program.id), programName = programEdition.program.name,
    programEditionName = programEdition.name, documentRequirements = documentRequirements,
)

fun Application.toAdminListItemResponse() = AdminApplicationListItemResponse(
    id = requireNotNull(id), applicationNumber = requireNotNull(applicationNumber),
    userId = requireNotNull(user.id), userName = user.name, userEmail = user.email,
    registeredByUserId = requireNotNull(registeredBy.id),
    programId = requireNotNull(programEdition.program.id), programName = programEdition.program.name,
    programEditionId = requireNotNull(programEdition.id), programEditionName = programEdition.name,
    enrollmentPeriodId = requireNotNull(enrollmentPeriod.id), status = status,
    assignedWorkerUserId = assignedWorker?.id, submittedAt = submittedAt, resolvedAt = resolvedAt,
    createdAt = createdAt, updatedAt = updatedAt,
)

fun Application.toAdminResponse(
    pendingDocuments: List<com.uade.dda2.server.feature.application.dto.response.PendingApplicationDocumentResponse> = emptyList(),
    documentRequirements: List<AvailableProgramDocumentRequirementResponse> = emptyList(),
) = AdminApplicationResponse(
    id = requireNotNull(id), applicationNumber = requireNotNull(applicationNumber),
    userId = requireNotNull(user.id), userName = user.name, userEmail = user.email,
    registeredByUserId = requireNotNull(registeredBy.id), registeredByUserName = registeredBy.name,
    programId = requireNotNull(programEdition.program.id), programName = programEdition.program.name,
    programEditionId = requireNotNull(programEdition.id), programEditionName = programEdition.name,
    enrollmentPeriodId = requireNotNull(enrollmentPeriod.id), status = status,
    originTicketId = originTicketId, resolutionReason = resolutionReason,
    assignedWorkerUserId = assignedWorker?.id, assignedWorkerName = assignedWorker?.name,
    submittedAt = submittedAt, resolvedAt = resolvedAt, createdAt = createdAt, updatedAt = updatedAt,
    idempotencyKey = idempotencyKey, requestHash = requestHash,
    pendingDocuments = pendingDocuments, documentRequirements = documentRequirements,
)

fun Application.toAuditSnapshot(): Map<String, Any?> = mapOf(
    "id" to id.toString(), "userId" to user.id, "applicationNumber" to applicationNumber,
    "registeredByUserId" to registeredBy.id,
    "programEditionId" to programEdition.id.toString(), "enrollmentPeriodId" to enrollmentPeriod.id.toString(),
    "status" to status.name, "submittedAt" to submittedAt.toString(),
)
