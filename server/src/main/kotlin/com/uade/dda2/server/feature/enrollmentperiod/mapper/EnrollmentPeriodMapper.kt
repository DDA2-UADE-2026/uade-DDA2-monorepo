package com.uade.dda2.server.feature.enrollmentperiod.mapper

import com.uade.dda2.server.feature.enrollmentperiod.dto.request.CreateEnrollmentPeriodRequest
import com.uade.dda2.server.feature.enrollmentperiod.dto.request.UpdateEnrollmentPeriodRequest
import com.uade.dda2.server.feature.enrollmentperiod.dto.response.EnrollmentPeriodListItemResponse
import com.uade.dda2.server.feature.enrollmentperiod.dto.response.EnrollmentPeriodListResponse
import com.uade.dda2.server.feature.enrollmentperiod.dto.response.EnrollmentPeriodResponse
import com.uade.dda2.server.feature.enrollmentperiod.entity.EnrollmentPeriod
import com.uade.dda2.server.feature.enrollmentperiod.entity.EnrollmentPeriodStatus
import com.uade.dda2.server.feature.program.entity.ProgramEdition
import org.springframework.data.domain.Page

fun CreateEnrollmentPeriodRequest.toEntity(
    programEdition: ProgramEdition,
): EnrollmentPeriod =
    EnrollmentPeriod(
        programEdition = programEdition,
        openDate = openDate,
        closeDate = closeDate,
        status = EnrollmentPeriodStatus.SCHEDULED,
        notes = notes?.trim()?.ifBlank { null },
    )

fun EnrollmentPeriod.updateFrom(request: UpdateEnrollmentPeriodRequest) {
    openDate = request.openDate
    closeDate = request.closeDate
    notes = request.notes?.trim()?.ifBlank { null }
}

fun EnrollmentPeriod.toResponse(): EnrollmentPeriodResponse =
    EnrollmentPeriodResponse(
        id = requireNotNull(id),
        programId = requireNotNull(programEdition.program.id),
        programName = programEdition.program.name,
        programEditionId = requireNotNull(programEdition.id),
        programEditionName = programEdition.name,
        openDate = openDate,
        closeDate = closeDate,
        status = status,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun EnrollmentPeriod.toListItemResponse(): EnrollmentPeriodListItemResponse =
    EnrollmentPeriodListItemResponse(
        id = requireNotNull(id),
        openDate = openDate,
        closeDate = closeDate,
        status = status,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun Page<EnrollmentPeriod>.toListResponse(): EnrollmentPeriodListResponse =
    EnrollmentPeriodListResponse(
        content = content.map { it.toListItemResponse() },
        page = number,
        size = size,
        totalElements = totalElements,
        totalPages = totalPages,
    )

fun EnrollmentPeriod.toAuditSnapshot(): Map<String, Any?> =
    mapOf(
        "id" to id,
        "programEditionId" to programEdition.id,
        "openDate" to openDate.toString(),
        "closeDate" to closeDate.toString(),
        "status" to status.name,
        "notes" to notes,
        "createdAt" to createdAt.toString(),
        "updatedAt" to updatedAt.toString(),
    )
