package com.uade.dda2.server.feature.program.dto.admin.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "Datos requeridos para actualizar un programa social.")
data class UpdateProgramRequest(

    @field:Schema(description = "Nombre actualizado del programa.", example = "Becas de formación y empleo")
    @field:NotBlank(message = "El nombre del programa es obligatorio.")
    @field:Size(
        max = 200,
        message = "El nombre del programa no puede superar los 200 caracteres.",
    )
    val name: String,

    @field:Schema(description = "Objetivo actualizado del programa.", example = "Promover la inserción laboral sostenible.", nullable = true)
    val objective: String? = null,
)
