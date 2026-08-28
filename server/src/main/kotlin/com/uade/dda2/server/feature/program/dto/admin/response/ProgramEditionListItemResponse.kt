package com.uade.dda2.server.feature.program.dto.admin.response

import com.uade.dda2.server.feature.program.entity.enums.ProgramEditionStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Schema(description = "Resumen de una edición incluida en un listado.")
data class ProgramEditionListItemResponse(
    @field:Schema(description = "UUID de la edición.", example = "650e8400-e29b-41d4-a716-446655440001", format = "uuid", accessMode = Schema.AccessMode.READ_ONLY)
    val id: UUID,

    @field:Schema(description = "UUID del programa.", example = "550e8400-e29b-41d4-a716-446655440000", format = "uuid", accessMode = Schema.AccessMode.READ_ONLY)
    val programId: UUID,
    @field:Schema(description = "Nombre del programa.", example = "Becas de formación laboral", accessMode = Schema.AccessMode.READ_ONLY)
    val programName: String,

    @field:Schema(description = "Nombre de la edición.", example = "Convocatoria 2026", accessMode = Schema.AccessMode.READ_ONLY)
    val name: String,

    @field:Schema(description = "Fecha de inicio.", example = "2026-03-01", format = "date", accessMode = Schema.AccessMode.READ_ONLY)
    val startDate: LocalDate,
    @field:Schema(description = "Fecha de finalización.", example = "2026-11-30", format = "date", accessMode = Schema.AccessMode.READ_ONLY)
    val endDate: LocalDate,

    @field:Schema(description = "Capacidad máxima de participantes.", example = "250", accessMode = Schema.AccessMode.READ_ONLY)
    val maxCapacity: Int,
    @field:Schema(description = "Cantidad actual de participantes inscriptos.", example = "87", accessMode = Schema.AccessMode.READ_ONLY)
    val currentEnrollment: Int,

    @field:Schema(description = "Estado actual de la edición.", example = "ACTIVE", accessMode = Schema.AccessMode.READ_ONLY)
    val status: ProgramEditionStatus,

    @field:Schema(description = "Fecha y hora de creación.", example = "2026-02-01T10:00:00", format = "date-time", accessMode = Schema.AccessMode.READ_ONLY)
    val createdAt: LocalDateTime,
    @field:Schema(description = "Fecha y hora de la última actualización.", example = "2026-08-20T15:30:00", format = "date-time", accessMode = Schema.AccessMode.READ_ONLY)
    val updatedAt: LocalDateTime,
)
