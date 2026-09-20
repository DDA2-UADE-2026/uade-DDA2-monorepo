package com.uade.dda2.server.feature.center.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

@Schema(description = "Datos requeridos para crear un servicio municipal.")
data class CreateMunicipalServiceRequest(
    @field:NotBlank(message = "El nombre del servicio es obligatorio.")
    @field:Size(max = 150, message = "El nombre del servicio no puede superar los 150 caracteres.")
    val name: String,

    @field:NotBlank(message = "La descripción del servicio es obligatoria.")
    @field:Size(max = 1000, message = "La descripción del servicio no puede superar los 1000 caracteres.")
    val description: String,

    @field:Positive(message = "La duración estimada debe ser mayor a cero.")
    val durationMinutes: Int,
)
