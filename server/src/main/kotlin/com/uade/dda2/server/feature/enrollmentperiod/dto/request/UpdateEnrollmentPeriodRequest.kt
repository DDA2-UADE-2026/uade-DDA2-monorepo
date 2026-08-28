package com.uade.dda2.server.feature.enrollmentperiod.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import java.time.LocalDate

@Schema(description = "Datos editables de un período de inscripción.")
data class UpdateEnrollmentPeriodRequest(
    @field:Schema(description = "Fecha inicial del período, inclusive.", example = "2026-09-01", format = "date")
    val openDate: LocalDate,

    @field:Schema(description = "Fecha final del período, inclusive.", example = "2026-10-15", format = "date")
    val closeDate: LocalDate,

    @field:Schema(description = "Observaciones administrativas opcionales.", example = "Plazo extendido.", nullable = true, maxLength = 1000)
    @field:Size(
        max = 1000,
        message = "Las observaciones no pueden superar los 1000 caracteres.",
    )
    val notes: String? = null,
)
