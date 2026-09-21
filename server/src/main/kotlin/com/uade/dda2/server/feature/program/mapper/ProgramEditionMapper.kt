package com.uade.dda2.server.feature.program.mapper

import com.uade.dda2.server.feature.auth.entity.User
import com.uade.dda2.server.feature.program.dto.admin.request.CreateProgramEditionRequest
import com.uade.dda2.server.feature.program.dto.admin.request.UpdateProgramEditionRequest
import com.uade.dda2.server.feature.program.dto.admin.response.ProgramEditionListItemResponse
import com.uade.dda2.server.feature.program.dto.admin.response.ProgramEditionListResponse
import com.uade.dda2.server.feature.program.dto.admin.response.ProgramEditionOptionResponse
import com.uade.dda2.server.feature.program.dto.admin.response.ProgramEditionResponse
import com.uade.dda2.server.feature.program.entity.Program
import com.uade.dda2.server.feature.program.entity.ProgramEdition
import com.uade.dda2.server.feature.program.entity.enums.ProgramEditionStatus
import org.springframework.data.domain.Page

fun CreateProgramEditionRequest.toEntity(
    program: Program,
    createdBy: User,
): ProgramEdition =
    ProgramEdition(
        program = program,
        name = name.trim(),
        normalizedName = name.trim().lowercase(),
        startDate = startDate,
        endDate = endDate,
        maxCapacity = maxCapacity,
        currentEnrollment = 0,
        status = ProgramEditionStatus.DRAFT,
        createdBy = createdBy,
    )

fun ProgramEdition.updateFrom(
    request: UpdateProgramEditionRequest,
) {
    name = request.name.trim()
    normalizedName = request.name.trim().lowercase()
    startDate = request.startDate
    endDate = request.endDate
    maxCapacity = request.maxCapacity
}

fun ProgramEdition.toResponse(): ProgramEditionResponse =
    ProgramEditionResponse(
        id = requireNotNull(id),
        programId = requireNotNull(program.id),
        programName = program.name,
        name = name,
        startDate = startDate,
        endDate = endDate,
        maxCapacity = maxCapacity,
        currentEnrollment = currentEnrollment,
        status = status,
        createdBy = createdBy.toProgramCreatedByResponse(),
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun ProgramEdition.toListItemResponse(): ProgramEditionListItemResponse =
    ProgramEditionListItemResponse(
        id = requireNotNull(id),
        programId = requireNotNull(program.id),
        programName = program.name,
        name = name,
        startDate = startDate,
        endDate = endDate,
        maxCapacity = maxCapacity,
        currentEnrollment = currentEnrollment,
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun Page<ProgramEdition>.toListResponse(): ProgramEditionListResponse =
    ProgramEditionListResponse(
        content = content.map { it.toListItemResponse() },
        page = number,
        size = size,
        totalElements = totalElements,
        totalPages = totalPages,
    )

fun ProgramEdition.toOptionResponse(): ProgramEditionOptionResponse =
    ProgramEditionOptionResponse(
        id = requireNotNull(id),
        name = name,
    )

fun ProgramEdition.toAuditSnapshot(): Map<String, Any?> =
    mapOf(
        "id" to id,
        "programId" to program.id,
        "name" to name,
        "startDate" to startDate.toString(),
        "endDate" to endDate.toString(),
        "maxCapacity" to maxCapacity,
        "currentEnrollment" to currentEnrollment,
        "status" to status.name,
        "createdBy" to createdBy.id,
        "createdAt" to createdAt.toString(),
        "updatedAt" to updatedAt.toString(),
    )
