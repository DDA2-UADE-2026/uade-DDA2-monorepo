package com.uade.dda2.server.feature.auth.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Resultado de un inicio de sesión exitoso.")
data class LoginResponse(
    @field:Schema(description = "Token JWT utilizado para autenticar solicitudes.", example = "eyJhbGciOiJIUzI1NiJ9...", accessMode = Schema.AccessMode.READ_ONLY)
    val token: String,
    @field:Schema(description = "Tiempo de validez del token, expresado en segundos.", example = "28800", accessMode = Schema.AccessMode.READ_ONLY)
    val expiresIn: Long,
    @field:Schema(description = "Datos del usuario autenticado.", accessMode = Schema.AccessMode.READ_ONLY)
    val user: UserResponse,
    @field:Schema(description = "Permisos efectivos concedidos al usuario.", example = "[\"users:view\"]", accessMode = Schema.AccessMode.READ_ONLY)
    val permissions: List<String>,
)
