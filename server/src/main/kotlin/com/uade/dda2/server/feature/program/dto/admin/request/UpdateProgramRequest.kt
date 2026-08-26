package com.uade.dda2.server.feature.program.dto.admin.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class UpdateProgramRequest(

    @field:NotBlank(message = "El nombre del programa es obligatorio.")
    @field:Size(
        max = 200,
        message = "El nombre del programa no puede superar los 200 caracteres.",
    )
    val name: String,

    val objective: String? = null,
)