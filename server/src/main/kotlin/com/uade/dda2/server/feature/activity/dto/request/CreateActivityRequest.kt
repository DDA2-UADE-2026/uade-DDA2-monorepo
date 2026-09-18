package com.uade.dda2.server.feature.activity.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.time.LocalDate

@Schema(description = "Datos requeridos para crear una actividad comunitaria.")
data class CreateActivityRequest(
    @field:Schema(description = "Nombre de la actividad.", example = "Taller comunitario de RCP")
    @field:NotBlank(message = "El nombre de la actividad es obligatorio.")
    @field:Size(max = 200, message = "El nombre de la actividad no puede superar los 200 caracteres.")
    val name: String,

    @field:Schema(description = "Descripción de la actividad.", example = "Capacitación abierta sobre técnicas básicas de reanimación.")
    @field:NotBlank(message = "La descripción de la actividad es obligatoria.")
    val description: String,

    @field:Schema(description = "Lugar donde se realizará la actividad.", example = "Centro Municipal Norte")
    @field:NotBlank(message = "El lugar de la actividad es obligatorio.")
    @field:Size(max = 300, message = "El lugar de la actividad no puede superar los 300 caracteres.")
    val location: String,

    @field:Schema(description = "Fecha inicial de la actividad, inclusive.", example = "2026-10-10", format = "date")
    val startDate: LocalDate,

    @field:Schema(description = "Fecha final de la actividad, inclusive.", example = "2026-10-10", format = "date")
    val endDate: LocalDate,

    @field:Schema(description = "Cantidad máxima de personas admitidas.", example = "30", minimum = "1")
    @field:Positive(message = "La capacidad debe ser mayor a cero.")
    val capacity: Int,
)
