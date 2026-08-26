package com.uade.dda2.server.feature.auth.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "Datos requeridos para actualizar un rol.")
data class UpdateRoleRequest(
    @field:Schema(description = "Nuevo nombre único del rol.", example = "SUPERVISOR")
    @field:NotBlank(message = "El nombre es obligatorio.")
    @field:Size(min = 1, max = 50, message = "El nombre no puede superar los 50 caracteres.")
    val name: String = "",

    @field:Schema(description = "Lista completa de permisos que conservará el rol.", example = "[\"users:view\", \"roles:view\"]")
    val permissions: List<String> = emptyList(),
)
