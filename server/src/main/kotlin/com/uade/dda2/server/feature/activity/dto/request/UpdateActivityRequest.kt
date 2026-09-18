package com.uade.dda2.server.feature.activity.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.time.LocalDate

@Schema(description = "Datos requeridos para actualizar una actividad comunitaria en borrador.")
data class UpdateActivityRequest(
    @field:NotBlank(message = "El nombre de la actividad es obligatorio.")
    @field:Size(max = 200, message = "El nombre de la actividad no puede superar los 200 caracteres.")
    val name: String,

    @field:NotBlank(message = "La descripción de la actividad es obligatoria.")
    val description: String,

    @field:NotBlank(message = "El lugar de la actividad es obligatorio.")
    @field:Size(max = 300, message = "El lugar de la actividad no puede superar los 300 caracteres.")
    val location: String,

    @field:Schema(format = "date", example = "2026-10-10")
    val startDate: LocalDate,

    @field:Schema(format = "date", example = "2026-10-10")
    val endDate: LocalDate,

    @field:Positive(message = "La capacidad debe ser mayor a cero.")
    val capacity: Int,
)
