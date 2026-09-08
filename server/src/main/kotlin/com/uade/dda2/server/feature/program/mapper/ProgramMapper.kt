package com.uade.dda2.server.feature.program.mapper

import com.uade.dda2.server.feature.auth.entity.User
import com.uade.dda2.server.feature.program.dto.admin.request.CreateProgramRequest
import com.uade.dda2.server.feature.program.dto.admin.request.UpdateProgramRequest
import com.uade.dda2.server.feature.program.dto.admin.response.ProgramCreatedByResponse
import com.uade.dda2.server.feature.program.dto.admin.response.ProgramListItemResponse
import com.uade.dda2.server.feature.program.dto.admin.response.ProgramListResponse
import com.uade.dda2.server.feature.program.dto.admin.response.ProgramOptionResponse
import com.uade.dda2.server.feature.program.dto.admin.response.ProgramResponse
import com.uade.dda2.server.feature.program.entity.Program
import org.springframework.data.domain.Page
import java.util.UUID

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

fun Program.toResponse(imageId: UUID? = null): ProgramResponse =
    ProgramResponse(
        id = requireNotNull(id),
        name = name,
        objective = objective,
        imageUrl = programImageUrl(imageId),
        createdBy = createdBy.toProgramCreatedByResponse(),
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun Program.toListItemResponse(
    active: Boolean,
    imageId: UUID? = null,
): ProgramListItemResponse =
    ProgramListItemResponse(
        id = requireNotNull(id),
        name = name,
        objective = objective,
        imageUrl = programImageUrl(imageId),
        active = active,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun Page<Program>.toListResponse(
    activeProgramIds: Set<UUID>,
    imageIdsByProgram: Map<UUID, UUID> = emptyMap(),
): ProgramListResponse =
    ProgramListResponse(
        content = content.map { program ->
            val programId = requireNotNull(program.id)
            program.toListItemResponse(
                active = programId in activeProgramIds,
                imageId = imageIdsByProgram[programId],
            )
        },
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

fun Program.toOptionResponse(imageId: UUID? = null): ProgramOptionResponse =
    ProgramOptionResponse(
        id = requireNotNull(id),
        name = name,
        imageUrl = programImageUrl(imageId),
    )
