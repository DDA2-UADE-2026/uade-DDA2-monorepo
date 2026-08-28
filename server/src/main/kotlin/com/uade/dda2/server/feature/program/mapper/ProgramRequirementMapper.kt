package com.uade.dda2.server.feature.program.mapper

import com.uade.dda2.server.feature.program.dto.admin.request.CreateProgramRequirementRequest
import com.uade.dda2.server.feature.program.dto.admin.request.UpdateProgramRequirementRequest
import com.uade.dda2.server.feature.program.dto.admin.response.ProgramRequirementResponse
import com.uade.dda2.server.feature.program.entity.ProgramEdition
import com.uade.dda2.server.feature.program.entity.ProgramRequirement

fun CreateProgramRequirementRequest.toEntity(
    programEdition: ProgramEdition,
): ProgramRequirement =
    ProgramRequirement(
        programEdition = programEdition,
        type = type,
        value = value.trim(),
        description = description?.trim(),
    )

fun ProgramRequirement.updateFrom(
    request: UpdateProgramRequirementRequest,
) {
    type = request.type
    value = request.value.trim()
    description = request.description?.trim()
}

fun ProgramRequirement.toResponse(): ProgramRequirementResponse =
    ProgramRequirementResponse(
        id = requireNotNull(id),
        programEditionId = requireNotNull(programEdition.id),
        type = type,
        value = value,
        description = description,
    )