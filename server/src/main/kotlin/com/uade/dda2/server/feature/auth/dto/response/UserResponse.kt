package com.uade.dda2.server.feature.auth.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Datos públicos de un usuario autenticado.")
data class UserResponse(
    @field:Schema(description = "Identificador único del usuario.", example = "12", accessMode = Schema.AccessMode.READ_ONLY)
    val id: Long,
    @field:Schema(description = "Nombre utilizado para iniciar sesión.", example = "maria.gomez", accessMode = Schema.AccessMode.READ_ONLY)
    val username: String,
    @field:Schema(description = "Nombre completo del usuario.", example = "María Gómez", accessMode = Schema.AccessMode.READ_ONLY)
    val name: String,
    @field:Schema(description = "Correo electrónico del usuario.", example = "maria.gomez@example.com", format = "email", accessMode = Schema.AccessMode.READ_ONLY)
    val email: String,
    @field:Schema(description = "Roles asignados al usuario.", example = "[\"OPERADOR\"]", accessMode = Schema.AccessMode.READ_ONLY)
    val roles: List<String>,
    @field:Schema(description = "Permisos efectivos del usuario.", example = "[\"users:view\"]", accessMode = Schema.AccessMode.READ_ONLY)
    val permissions: List<String>,
)
