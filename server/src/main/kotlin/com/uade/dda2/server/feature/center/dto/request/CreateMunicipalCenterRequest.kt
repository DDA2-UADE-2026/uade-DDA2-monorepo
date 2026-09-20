package com.uade.dda2.server.feature.center.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "Datos requeridos para crear un centro municipal.")
data class CreateMunicipalCenterRequest(
    @field:NotBlank(message = "El nombre del centro es obligatorio.")
    @field:Size(max = 150, message = "El nombre del centro no puede superar los 150 caracteres.")
    val name: String,

    @field:NotBlank(message = "La dirección del centro es obligatoria.")
    @field:Size(max = 255, message = "La dirección del centro no puede superar los 255 caracteres.")
    val address: String,

    @field:Size(max = 30, message = "El teléfono del centro no puede superar los 30 caracteres.")
    val phone: String? = null,

    @field:Email(message = "El correo electrónico del centro no tiene un formato válido.")
    @field:Size(max = 180, message = "El correo electrónico del centro no puede superar los 180 caracteres.")
    val email: String? = null,
)
