package com.uade.dda2.server.feature.auth.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "Cambio de rol activo. Requiere un bearer JWT operativo y no revoca el token anterior.")
data class SwitchRoleRequest(
    @field:Schema(description = "Nombre exacto de un rol actualmente asignado al usuario.", example = "CIUDADANO")
    @field:NotBlank
    @field:Size(max = 50)
    val role: String = "",
)
