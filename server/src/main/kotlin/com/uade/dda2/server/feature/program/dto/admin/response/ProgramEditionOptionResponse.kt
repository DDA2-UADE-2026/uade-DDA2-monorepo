package com.uade.dda2.server.feature.program.dto.admin.response

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "Opción reducida de una edición para controles de selección.")
data class ProgramEditionOptionResponse(
    @field:Schema(description = "UUID de la edición.", example = "650e8400-e29b-41d4-a716-446655440001", format = "uuid", accessMode = Schema.AccessMode.READ_ONLY)
    val id: UUID,
    @field:Schema(description = "Nombre de la edición.", example = "Convocatoria 2026", accessMode = Schema.AccessMode.READ_ONLY)
    val name: String,
)
