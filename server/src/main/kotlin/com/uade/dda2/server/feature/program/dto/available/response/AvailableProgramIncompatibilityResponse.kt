package com.uade.dda2.server.feature.program.dto.available.response

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "Programa incompatible con el programa consultado.")
data class AvailableProgramIncompatibilityResponse(
    @field:Schema(description = "UUID del programa incompatible.", format = "uuid", accessMode = Schema.AccessMode.READ_ONLY)
    val id: UUID,
    @field:Schema(description = "Nombre del programa incompatible.", example = "Subsidio de empleo joven", accessMode = Schema.AccessMode.READ_ONLY)
    val name: String,
)
