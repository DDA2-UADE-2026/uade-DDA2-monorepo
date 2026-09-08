package com.uade.dda2.server.feature.log.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Usuario que originó el evento auditado.")
data class LogActorResponse(
    @field:Schema(description = "Identificador del usuario.", example = "12", accessMode = Schema.AccessMode.READ_ONLY)
    val id: Long,
    @field:Schema(description = "Nombre de usuario local.", example = "maria.gomez", nullable = true, accessMode = Schema.AccessMode.READ_ONLY)
    val username: String?,
    @field:Schema(description = "Nombre completo del usuario.", example = "María Gómez", accessMode = Schema.AccessMode.READ_ONLY)
    val name: String,
)
