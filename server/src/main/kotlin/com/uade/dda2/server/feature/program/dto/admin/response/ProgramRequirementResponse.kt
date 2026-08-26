package com.uade.dda2.server.feature.program.dto.admin.response

import com.uade.dda2.server.feature.program.entity.enums.ProgramRequirementType
import java.util.UUID

data class ProgramRequirementResponse(
    val id: UUID,
    val programEditionId: UUID,
    val type: ProgramRequirementType,
    val value: String,
    val description: String?,
)