package com.uade.dda2.server.feature.program.dto.admin.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.time.LocalDate

@Schema(description = "Datos requeridos para crear una edición de un programa.")
data class CreateProgramEditionRequest(

    @field:Schema(description = "Nombre de la edición.", example = "Convocatoria 2026")
    @field:NotBlank(message = "El nombre de la edición es obligatorio.")
    @field:Size(
        max = 200,
        message = "El nombre de la edición no puede superar los 200 caracteres.",
    )
    val name: String,

    @field:Schema(description = "Fecha de inicio de la edición.", example = "2026-03-01", format = "date")
    val startDate: LocalDate,

    @field:Schema(description = "Fecha de finalización de la edición.", example = "2026-11-30", format = "date")
    val endDate: LocalDate,

    @field:Schema(description = "Cantidad máxima de participantes admitidos.", example = "250", minimum = "1")
    @field:Positive(message = "La capacidad máxima debe ser mayor a cero.")
    val maxCapacity: Int,
)
