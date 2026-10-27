package com.uade.dda2.server.feature.application.mapper

import com.uade.dda2.server.feature.application.dto.response.ApplicationEvaluationResponse
import com.uade.dda2.server.feature.application.dto.response.ApplicationEvaluationDetailResponse
import com.uade.dda2.server.feature.application.dto.response.ApplicationEvaluationListItemResponse
import com.uade.dda2.server.feature.application.entity.ApplicationEvaluation
import com.uade.dda2.server.feature.application.entity.ApplicationStatus

fun ApplicationEvaluation.toResponse(): ApplicationEvaluationResponse = ApplicationEvaluationResponse(
    id = requireNotNull(id),
    applicationId = requireNotNull(application.id),
    vulnerability = vulnerability,
    priority = priority,
    recommendation = recommendation,
    professionalId = requireNotNull(professional.id),
    professionalName = professional.name,
    evaluatedAt = evaluatedAt,
)

fun ApplicationEvaluation.toListItemResponse(): ApplicationEvaluationListItemResponse = ApplicationEvaluationListItemResponse(
    id = requireNotNull(id),
    applicationId = requireNotNull(application.id),
    applicationNumber = requireNotNull(application.applicationNumber),
    applicationStatus = application.status,
    applicantUserId = requireNotNull(application.user.id),
    applicantName = application.user.name,
    programId = requireNotNull(application.programEdition.program.id),
    programName = application.programEdition.program.name,
    programEditionId = requireNotNull(application.programEdition.id),
    programEditionName = application.programEdition.name,
    vulnerability = vulnerability,
    priority = priority,
    professionalId = requireNotNull(professional.id),
    professionalName = professional.name,
    evaluatedAt = evaluatedAt,
)

fun ApplicationEvaluation.toDetailResponse(): ApplicationEvaluationDetailResponse = ApplicationEvaluationDetailResponse(
    id = requireNotNull(id),
    applicationId = requireNotNull(application.id),
    applicationNumber = requireNotNull(application.applicationNumber),
    applicationStatus = application.status,
    applicantUserId = requireNotNull(application.user.id),
    applicantName = application.user.name,
    applicantEmail = application.user.email,
    programId = requireNotNull(application.programEdition.program.id),
    programName = application.programEdition.program.name,
    programEditionId = requireNotNull(application.programEdition.id),
    programEditionName = application.programEdition.name,
    enrollmentPeriodId = requireNotNull(application.enrollmentPeriod.id),
    vulnerability = vulnerability,
    priority = priority,
    recommendation = recommendation,
    professionalId = requireNotNull(professional.id),
    professionalName = professional.name,
    professionalEmail = professional.email,
    evaluatedAt = evaluatedAt,
)

fun ApplicationEvaluation.toAuditSnapshot(previousStatus: ApplicationStatus): Map<String, Any?> = mapOf(
    "id" to id.toString(),
    "applicationId" to application.id.toString(),
    "vulnerability" to vulnerability.name,
    "priority" to priority.name,
    "professionalId" to professional.id,
    "evaluatedAt" to evaluatedAt.toString(),
    "previousApplicationStatus" to previousStatus.name,
    "applicationStatus" to application.status.name,
)
