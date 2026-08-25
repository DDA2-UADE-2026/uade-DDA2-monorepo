package com.uade.dda2.server.feature.program.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.time.LocalDate

data class UpdateProgramEditionRequest(

    @field:NotBlank(message = "El nombre de la edición es obligatorio.")
    @field:Size(
        max = 200,
        message = "El nombre de la edición no puede superar los 200 caracteres.",
    )
    val name: String,

    val startDate: LocalDate,

    val endDate: LocalDate,

    @field:Positive(message = "La capacidad máxima debe ser mayor a cero.")
    val maxCapacity: Int,
)