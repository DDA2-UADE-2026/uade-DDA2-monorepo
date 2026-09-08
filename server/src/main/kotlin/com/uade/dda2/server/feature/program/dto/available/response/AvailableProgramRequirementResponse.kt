package com.uade.dda2.server.feature.program.dto.available.response

import com.uade.dda2.server.feature.program.entity.enums.ProgramRequirementType
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "Requisito exigido por una edición disponible.")
data class AvailableProgramRequirementResponse(
    @field:Schema(description = "UUID del requisito.", format = "uuid", accessMode = Schema.AccessMode.READ_ONLY)
    val id: UUID,
    @field:Schema(description = "Tipo de requisito.", example = "MIN_AGE", accessMode = Schema.AccessMode.READ_ONLY)
    val type: ProgramRequirementType,
    @field:Schema(description = "Valor que debe cumplir el ciudadano.", example = "18", accessMode = Schema.AccessMode.READ_ONLY)
    val value: String,
    @field:Schema(description = "Explicación complementaria del requisito.", nullable = true, accessMode = Schema.AccessMode.READ_ONLY)
    val description: String?,
)
