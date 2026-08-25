package com.uade.dda2.server.feature.program.mapper

import com.uade.dda2.server.feature.auth.entity.User
import com.uade.dda2.server.feature.program.dto.request.CreateProgramRequest
import com.uade.dda2.server.feature.program.dto.request.UpdateProgramRequest
import com.uade.dda2.server.feature.program.dto.response.ProgramCreatedByResponse
import com.uade.dda2.server.feature.program.dto.response.ProgramListItemResponse
import com.uade.dda2.server.feature.program.dto.response.ProgramListResponse
import com.uade.dda2.server.feature.program.dto.response.ProgramOptionResponse
import com.uade.dda2.server.feature.program.dto.response.ProgramResponse
import com.uade.dda2.server.feature.program.entity.Program
import org.springframework.data.domain.Page

fun CreateProgramRequest.toEntity(
    createdBy: User,
): Program =
    Program(
        name = name.trim(),
        normalizedName = name.trim().lowercase(),
        objective = objective?.trim(),
        createdBy = createdBy,
    )

fun Program.updateFrom(
    request: UpdateProgramRequest,
) {
    name = request.name.trim()
    normalizedName = request.name.trim().lowercase()
    objective = request.objective?.trim()
}

fun Program.toResponse(): ProgramResponse =
    ProgramResponse(
        id = requireNotNull(id),
        name = name,
        objective = objective,
        createdBy = createdBy.toProgramCreatedByResponse(),
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun Program.toListItemResponse(): ProgramListItemResponse =
    ProgramListItemResponse(
        id = requireNotNull(id),
        name = name,
        objective = objective,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun Page<Program>.toListResponse(): ProgramListResponse =
    ProgramListResponse(
        content = content.map { it.toListItemResponse() },
        page = number,
        size = size,
        totalElements = totalElements,
        totalPages = totalPages,
    )

fun User.toProgramCreatedByResponse(): ProgramCreatedByResponse =
    ProgramCreatedByResponse(
        id = requireNotNull(id),
        name = name,
    )

fun Program.toOptionResponse(): ProgramOptionResponse =
    ProgramOptionResponse(
        id = requireNotNull(id),
        name = name,
    )
