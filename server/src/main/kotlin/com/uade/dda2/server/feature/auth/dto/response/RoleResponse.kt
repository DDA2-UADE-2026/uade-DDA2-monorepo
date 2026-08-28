package com.uade.dda2.server.feature.auth.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Rol y permisos asociados.")
data class RoleResponse(
    @field:Schema(description = "Identificador único del rol.", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    val id: Long,
    @field:Schema(description = "Nombre del rol.", example = "OPERADOR", accessMode = Schema.AccessMode.READ_ONLY)
    val name: String,
    @field:Schema(description = "Permisos asignados al rol.", example = "[\"users:view\", \"programs:management:view\"]", accessMode = Schema.AccessMode.READ_ONLY)
    val permissions: List<String>,
)
