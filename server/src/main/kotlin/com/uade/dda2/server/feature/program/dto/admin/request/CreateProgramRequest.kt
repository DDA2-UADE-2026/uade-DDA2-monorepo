package com.uade.dda2.server.feature.program.dto.admin.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "Datos requeridos para crear un programa social.")
data class CreateProgramRequest(

    @field:Schema(description = "Nombre del programa.", example = "Becas de formación laboral")
    @field:NotBlank(message = "El nombre del programa es obligatorio.")
    @field:Size(
        max = 200,
        message = "El nombre del programa no puede superar los 200 caracteres.",
    )
    val name: String,

    @field:Schema(description = "Objetivo que persigue el programa.", example = "Mejorar la empleabilidad de jóvenes.", nullable = true)
    val objective: String? = null,
)
