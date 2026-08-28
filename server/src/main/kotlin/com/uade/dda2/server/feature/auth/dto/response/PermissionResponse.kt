package com.uade.dda2.server.feature.auth.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Permiso disponible en el sistema.")
data class PermissionResponse(
    @field:Schema(description = "Identificador único del permiso.", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    val id: Long,
    @field:Schema(description = "Nombre técnico del permiso.", example = "users:view", accessMode = Schema.AccessMode.READ_ONLY)
    val name: String,
)
