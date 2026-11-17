package com.uade.dda2.server.feature.auth.dto.response

import com.uade.dda2.server.security.JwtAuthSource
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Datos públicos de un usuario autenticado.")
data class UserResponse(
    @field:Schema(description = "Identificador único del usuario.", example = "12", accessMode = Schema.AccessMode.READ_ONLY)
    val id: Long,
    @field:Schema(description = "Username local; puede no existir para una futura identidad externa.", example = "maria.gomez", nullable = true, accessMode = Schema.AccessMode.READ_ONLY)
    val username: String?,
    @field:Schema(description = "Nombre completo del usuario.", example = "María Gómez", accessMode = Schema.AccessMode.READ_ONLY)
    val name: String,
    @field:Schema(description = "Correo electrónico del usuario.", example = "maria.gomez@example.com", format = "email", accessMode = Schema.AccessMode.READ_ONLY)
    val email: String,
    @field:Schema(description = "Roles asignados al usuario.", example = "[\"OPERADOR\"]", accessMode = Schema.AccessMode.READ_ONLY)
    val roles: List<String>,
    @field:Schema(description = "Rol activo del JWT. Null mientras está pendiente la selección.", example = "AUDITOR", nullable = true, accessMode = Schema.AccessMode.READ_ONLY)
    val activeRole: String?,
    @field:Schema(description = "Permisos del rol activo incluidos en el JWT; no suma los demás roles.", example = "[\"users:view\"]", accessMode = Schema.AccessMode.READ_ONLY)
    val permissions: List<String>,
    @field:Schema(description = "Origen de la sesión: LOCAL para usuario y contraseña de M8, M1 para el ingreso con Ciudadanos.", example = "LOCAL", accessMode = Schema.AccessMode.READ_ONLY)
    val authSource: JwtAuthSource,
    @field:Schema(description = "citizenId de Ciudadanos (M1) vinculado a la cuenta. Null si la cuenta no tiene vínculo.", example = "b63f5a2e-4b71-46e7-8b01-381c85d88f62", format = "uuid", nullable = true, accessMode = Schema.AccessMode.READ_ONLY)
    val externalCitizenId: String?,
)
