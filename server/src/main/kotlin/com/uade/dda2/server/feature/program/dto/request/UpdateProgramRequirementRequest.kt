package com.uade.dda2.server.feature.program.dto.request

import com.uade.dda2.server.feature.program.entity.enums.ProgramRequirementType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class UpdateProgramRequirementRequest(

    val type: ProgramRequirementType,

    @field:NotBlank(message = "El valor del requisito es obligatorio.")
    @field:Size(
        max = 255,
        message = "El valor del requisito no puede superar los 255 caracteres.",
    )
    val value: String,

    @field:Size(
        max = 500,
        message = "La descripción no puede superar los 500 caracteres.",
    )
    val description: String? = null,
)