package com.uade.dda2.server.feature.auth.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Perfil del usuario autenticado.")
data class MeResponse(
    @field:Schema(description = "Datos del usuario autenticado.", accessMode = Schema.AccessMode.READ_ONLY)
    val user: UserResponse,
)
