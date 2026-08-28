package com.uade.dda2.server.feature.program.dto.available.response

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "Detalle de un programa disponible para ciudadanos.")
data class AvailableProgramDetailResponse(
    @field:Schema(description = "UUID del programa.", format = "uuid", accessMode = Schema.AccessMode.READ_ONLY)
    val id: UUID,
    @field:Schema(description = "Nombre del programa.", example = "Becas de formación laboral", accessMode = Schema.AccessMode.READ_ONLY)
    val name: String,
    @field:Schema(description = "Objetivo del programa.", example = "Mejorar la empleabilidad de jóvenes.", nullable = true, accessMode = Schema.AccessMode.READ_ONLY)
    val objective: String?,
    @field:Schema(description = "Ediciones vigentes o futuras disponibles para el ciudadano.", accessMode = Schema.AccessMode.READ_ONLY)
    val editions: List<AvailableProgramEditionResponse>,
    @field:Schema(description = "Programas incompatibles con el programa consultado.", accessMode = Schema.AccessMode.READ_ONLY)
    val incompatibilities: List<AvailableProgramIncompatibilityResponse>,
)
