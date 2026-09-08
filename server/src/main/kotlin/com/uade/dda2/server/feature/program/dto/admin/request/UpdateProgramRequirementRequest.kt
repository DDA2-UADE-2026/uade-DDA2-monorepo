package com.uade.dda2.server.feature.program.dto.admin.request

import com.uade.dda2.server.feature.program.entity.enums.ProgramRequirementType
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "Datos requeridos para actualizar un requisito.")
data class UpdateProgramRequirementRequest(

    @field:Schema(description = "Tipo actualizado del requisito.", example = "MIN_AGE")
    val type: ProgramRequirementType,

    @field:Schema(description = "Valor actualizado que debe cumplir el postulante.", example = "18-30")
    @field:NotBlank(message = "El valor del requisito es obligatorio.")
    @field:Size(
        max = 255,
        message = "El valor del requisito no puede superar los 255 caracteres.",
    )
    val value: String,

    @field:Schema(description = "Explicación actualizada del requisito.", example = "Tener entre 18 y 30 años al momento de la inscripción.", nullable = true)
    @field:Size(
        max = 500,
        message = "La descripción no puede superar los 500 caracteres.",
    )
    val description: String? = null,
)
