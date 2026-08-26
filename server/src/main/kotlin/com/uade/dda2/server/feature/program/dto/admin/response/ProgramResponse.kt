package com.uade.dda2.server.feature.program.dto.admin.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.util.UUID

@Schema(description = "Detalle completo de un programa social.")
data class ProgramResponse(
    @field:Schema(description = "UUID del programa.", example = "550e8400-e29b-41d4-a716-446655440000", format = "uuid", accessMode = Schema.AccessMode.READ_ONLY)
    val id: UUID,
    @field:Schema(description = "Nombre del programa.", example = "Becas de formación laboral", accessMode = Schema.AccessMode.READ_ONLY)
    val name: String,
    @field:Schema(description = "Objetivo del programa.", example = "Mejorar la empleabilidad de jóvenes.", nullable = true, accessMode = Schema.AccessMode.READ_ONLY)
    val objective: String?,
    @field:Schema(description = "Usuario que creó el programa.", accessMode = Schema.AccessMode.READ_ONLY)
    val createdBy: ProgramCreatedByResponse,
    @field:Schema(description = "Fecha y hora de creación.", example = "2026-02-01T10:00:00", format = "date-time", accessMode = Schema.AccessMode.READ_ONLY)
    val createdAt: LocalDateTime,
    @field:Schema(description = "Fecha y hora de la última actualización.", example = "2026-08-20T15:30:00", format = "date-time", accessMode = Schema.AccessMode.READ_ONLY)
    val updatedAt: LocalDateTime,
)
