package com.uade.dda2.server.feature.program.mapper

import com.uade.dda2.server.feature.program.dto.admin.request.CreateProgramDocumentRequirementRequest
import com.uade.dda2.server.feature.program.dto.admin.request.UpdateProgramDocumentRequirementRequest
import com.uade.dda2.server.feature.program.dto.admin.response.ProgramDocumentRequirementResponse
import com.uade.dda2.server.feature.program.dto.available.response.AvailableProgramDocumentRequirementResponse
import com.uade.dda2.server.feature.program.entity.ProgramDocumentRequirement
import com.uade.dda2.server.feature.program.entity.ProgramEdition

fun CreateProgramDocumentRequirementRequest.toEntity(programEdition: ProgramEdition) =
    ProgramDocumentRequirement(
        programEdition = programEdition,
        code = code.trim().uppercase(),
        name = name.trim(),
        description = description?.trim()?.takeIf(String::isNotEmpty),
        required = required,
    )

fun ProgramDocumentRequirement.updateFrom(request: UpdateProgramDocumentRequirementRequest) {
    code = request.code.trim().uppercase()
    name = request.name.trim()
    description = request.description?.trim()?.takeIf(String::isNotEmpty)
    required = request.required
}

fun ProgramDocumentRequirement.toDocumentRequirementResponse() = ProgramDocumentRequirementResponse(
    id = requireNotNull(id),
    programEditionId = requireNotNull(programEdition.id),
    code = code,
    name = name,
    description = description,
    required = required,
)

fun ProgramDocumentRequirement.toAvailableDocumentRequirementResponse() =
    AvailableProgramDocumentRequirementResponse(
        id = requireNotNull(id),
        code = code,
        name = name,
        description = description,
        required = required,
    )
