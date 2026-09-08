package com.uade.dda2.server.feature.auth.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Resultado de autenticación: JWT operativo con un rol activo o JWT temporal para seleccionar rol.")
data class LoginResponse(
    @field:Schema(description = "JWT operativo. Null mientras se requiere seleccionar rol.", nullable = true, accessMode = Schema.AccessMode.READ_ONLY)
    val token: String?,
    @field:Schema(description = "Tiempo de validez del JWT operativo en segundos; null mientras se requiere selección.", example = "28800", nullable = true, accessMode = Schema.AccessMode.READ_ONLY)
    val expiresIn: Long?,
    @field:Schema(description = "Datos del usuario autenticado.", accessMode = Schema.AccessMode.READ_ONLY)
    val user: UserResponse,
    @field:Schema(description = "Indica que falta seleccionar uno de los roles asignados.", accessMode = Schema.AccessMode.READ_ONLY)
    val requiresRoleSelection: Boolean = false,
    @field:Schema(description = "JWT temporal utilizable únicamente en POST /auth/select-role.", nullable = true, accessMode = Schema.AccessMode.READ_ONLY)
    val selectionToken: String? = null,
    @field:Schema(description = "Validez del JWT de selección en segundos; null para tokens operativos.", example = "300", nullable = true, accessMode = Schema.AccessMode.READ_ONLY)
    val selectionExpiresIn: Long? = null,
)
