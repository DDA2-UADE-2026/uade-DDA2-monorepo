package com.uade.dda2.server.feature.program.dto.admin.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

@Schema(description = "Configuración de un documento solicitado por una edición.")
data class CreateProgramDocumentRequirementRequest(
    @field:NotBlank @field:Size(max = 50)
    @field:Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]*$", message = "El código debe comenzar con una letra y contener solo letras, números o guion bajo.")
    @field:Schema(example = "DNI_FRONT")
    val code: String,
    @field:NotBlank @field:Size(max = 150)
    @field:Schema(example = "Frente del DNI")
    val name: String,
    @field:Size(max = 500)
    val description: String? = null,
    val required: Boolean,
)

@Schema(description = "Actualización de un documento solicitado por una edición.")
data class UpdateProgramDocumentRequirementRequest(
    @field:NotBlank @field:Size(max = 50)
    @field:Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]*$", message = "El código debe comenzar con una letra y contener solo letras, números o guion bajo.")
    val code: String,
    @field:NotBlank @field:Size(max = 150)
    val name: String,
    @field:Size(max = 500)
    val description: String? = null,
    val required: Boolean,
)
