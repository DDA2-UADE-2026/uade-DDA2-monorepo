package com.uade.dda2.server.feature.program.dto.admin.response

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "Relación de incompatibilidad entre dos programas.")
data class ProgramIncompatibilityResponse(
    @field:Schema(description = "UUID del programa de origen.", example = "550e8400-e29b-41d4-a716-446655440000", format = "uuid", accessMode = Schema.AccessMode.READ_ONLY)
    val programId: UUID,
    @field:Schema(description = "Nombre del programa de origen.", example = "Becas de formación laboral", accessMode = Schema.AccessMode.READ_ONLY)
    val programName: String,
    @field:Schema(description = "UUID del programa incompatible.", example = "950e8400-e29b-41d4-a716-446655440004", format = "uuid", accessMode = Schema.AccessMode.READ_ONLY)
    val incompatibleWithProgramId: UUID,
    @field:Schema(description = "Nombre del programa incompatible.", example = "Subsidio de empleo joven", accessMode = Schema.AccessMode.READ_ONLY)
    val incompatibleWithProgramName: String,
)
