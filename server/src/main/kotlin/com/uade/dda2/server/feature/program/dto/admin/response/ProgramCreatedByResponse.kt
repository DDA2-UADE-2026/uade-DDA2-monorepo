package com.uade.dda2.server.feature.program.dto.admin.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Referencia al usuario que creó el registro.")
data class ProgramCreatedByResponse(
    @field:Schema(description = "Identificador del usuario creador.", example = "12", accessMode = Schema.AccessMode.READ_ONLY)
    val id: Long,
    @field:Schema(description = "Nombre del usuario creador.", example = "María Gómez", accessMode = Schema.AccessMode.READ_ONLY)
    val name: String,
)
