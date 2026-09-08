package com.uade.dda2.server.feature.program.dto.admin.response

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "Opción reducida de un programa para controles de selección.")
data class ProgramOptionResponse(
    @field:Schema(description = "UUID del programa.", example = "550e8400-e29b-41d4-a716-446655440000", format = "uuid", accessMode = Schema.AccessMode.READ_ONLY)
    val id: UUID,
    @field:Schema(description = "Nombre del programa.", example = "Becas de formación laboral", accessMode = Schema.AccessMode.READ_ONLY)
    val name: String,
)
