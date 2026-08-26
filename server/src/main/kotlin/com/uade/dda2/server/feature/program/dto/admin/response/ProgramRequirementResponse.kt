package com.uade.dda2.server.feature.program.dto.admin.response

import com.uade.dda2.server.feature.program.entity.enums.ProgramRequirementType
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "Requisito asociado a una edición de programa.")
data class ProgramRequirementResponse(
    @field:Schema(description = "UUID del requisito.", example = "850e8400-e29b-41d4-a716-446655440003", format = "uuid", accessMode = Schema.AccessMode.READ_ONLY)
    val id: UUID,
    @field:Schema(description = "UUID de la edición que exige el requisito.", example = "650e8400-e29b-41d4-a716-446655440001", format = "uuid", accessMode = Schema.AccessMode.READ_ONLY)
    val programEditionId: UUID,
    @field:Schema(description = "Tipo de requisito.", example = "MIN_AGE", accessMode = Schema.AccessMode.READ_ONLY)
    val type: ProgramRequirementType,
    @field:Schema(description = "Valor que debe cumplir el postulante.", example = "18-25", accessMode = Schema.AccessMode.READ_ONLY)
    val value: String,
    @field:Schema(description = "Explicación complementaria del requisito.", example = "Tener entre 18 y 25 años.", nullable = true, accessMode = Schema.AccessMode.READ_ONLY)
    val description: String?,
)
