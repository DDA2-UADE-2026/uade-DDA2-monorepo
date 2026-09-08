package com.uade.dda2.server.feature.auth.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "Detalle administrativo de un usuario.")
data class UserManagementResponse(
    @field:Schema(description = "Identificador único del usuario.", example = "12", accessMode = Schema.AccessMode.READ_ONLY)
    val id: Long,
    @field:Schema(description = "Username local, ausente en una futura cuenta exclusivamente externa.", example = "maria.gomez", nullable = true, accessMode = Schema.AccessMode.READ_ONLY)
    val username: String?,
    @field:Schema(description = "Nombre completo del usuario.", example = "María Gómez", accessMode = Schema.AccessMode.READ_ONLY)
    val name: String,
    @field:Schema(description = "Correo electrónico del usuario.", example = "maria.gomez@example.com", format = "email", accessMode = Schema.AccessMode.READ_ONLY)
    val email: String,
    @field:Schema(description = "Indica si el usuario puede acceder al sistema.", example = "true", accessMode = Schema.AccessMode.READ_ONLY)
    val active: Boolean,
    @field:Schema(description = "Roles asignados al usuario.", example = "[\"OPERADOR\"]", accessMode = Schema.AccessMode.READ_ONLY)
    val roles: List<String>,
    @field:Schema(description = "Permisos asignados, separados por rol. No representan permisos de un JWT operativo.", accessMode = Schema.AccessMode.READ_ONLY)
    val permissionsByRole: Map<String, List<String>>,
    @field:Schema(description = "Indica si el usuario tiene username y contraseña locales.", accessMode = Schema.AccessMode.READ_ONLY)
    val hasLocalCredentials: Boolean,
    @field:Schema(description = "Vínculo reservado para Ciudadanos. No se puede modificar mediante el ABM de usuarios.", nullable = true, accessMode = Schema.AccessMode.READ_ONLY)
    val externalCitizenId: String?,
    @field:Schema(description = "Instante UTC de creación.", example = "2026-03-01T12:00:00Z", format = "date-time", accessMode = Schema.AccessMode.READ_ONLY)
    val createdAt: Instant,
    @field:Schema(description = "Instante UTC de la última actualización.", example = "2026-08-26T14:30:00Z", format = "date-time", nullable = true, accessMode = Schema.AccessMode.READ_ONLY)
    val updatedAt: Instant?,
)
