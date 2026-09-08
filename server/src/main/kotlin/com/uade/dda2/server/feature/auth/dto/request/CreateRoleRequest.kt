package com.uade.dda2.server.feature.auth.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "Datos requeridos para crear un rol.")
data class CreateRoleRequest(
    @field:Schema(description = "Nombre único del rol.", example = "OPERADOR")
    @field:NotBlank(message = "El nombre es obligatorio.")
    @field:Size(min = 1, max = 50, message = "El nombre no puede superar los 50 caracteres.")
    val name: String = "",

    @field:Schema(description = "Nombres de los permisos que se asignarán al rol.", example = "[\"users:view\", \"programs:management:view\"]")
    val permissions: List<String> = emptyList(),
)
