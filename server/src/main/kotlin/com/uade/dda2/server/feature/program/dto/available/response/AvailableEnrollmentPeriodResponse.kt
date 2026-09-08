package com.uade.dda2.server.feature.program.dto.available.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.util.UUID

@Schema(description = "Período en el que una edición se encuentra abierta para recibir solicitudes.")
data class AvailableEnrollmentPeriodResponse(
    @field:Schema(description = "UUID del período de inscripción.", format = "uuid", accessMode = Schema.AccessMode.READ_ONLY)
    val id: UUID,
    @field:Schema(description = "Fecha inicial del período, inclusive.", format = "date", accessMode = Schema.AccessMode.READ_ONLY)
    val openDate: LocalDate,
    @field:Schema(description = "Fecha final del período, inclusive.", format = "date", accessMode = Schema.AccessMode.READ_ONLY)
    val closeDate: LocalDate,
)
