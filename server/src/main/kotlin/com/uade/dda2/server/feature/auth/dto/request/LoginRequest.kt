package com.uade.dda2.server.feature.auth.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "Credenciales requeridas para iniciar sesión.")
data class LoginRequest(
    @field:Schema(description = "Nombre de usuario registrado.", example = "maria.gomez")
    @field:NotBlank(message = "El nombre de usuario es obligatorio.")
    val username: String = "",
    @field:NotBlank(message = "La contraseña es obligatoria.")
    val password: String = "",
)
    @field:Schema(description = "Contraseña del usuario.", example = "ClaveSegura123!", accessMode = Schema.AccessMode.WRITE_ONLY)
