package com.uade.dda2.server.feature.enrollmentperiod.dto.response

import com.uade.dda2.server.feature.enrollmentperiod.entity.EnrollmentPeriodStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Schema(description = "Resumen de un período de inscripción incluido en un listado.")
data class EnrollmentPeriodListItemResponse(
    @field:Schema(description = "UUID del período.", format = "uuid", accessMode = Schema.AccessMode.READ_ONLY)
    val id: UUID,
    @field:Schema(description = "Fecha inicial del período, inclusive.", example = "2026-09-01", format = "date", accessMode = Schema.AccessMode.READ_ONLY)
    val openDate: LocalDate,
    @field:Schema(description = "Fecha final del período, inclusive.", example = "2026-09-30", format = "date", accessMode = Schema.AccessMode.READ_ONLY)
    val closeDate: LocalDate,
    @field:Schema(description = "Estado actual del período.", example = "SCHEDULED", accessMode = Schema.AccessMode.READ_ONLY)
    val status: EnrollmentPeriodStatus,
    @field:Schema(description = "Observaciones administrativas.", nullable = true, accessMode = Schema.AccessMode.READ_ONLY)
    val notes: String?,
    @field:Schema(description = "Fecha y hora de creación.", format = "date-time", accessMode = Schema.AccessMode.READ_ONLY)
    val createdAt: LocalDateTime,
    @field:Schema(description = "Fecha y hora de la última actualización.", format = "date-time", accessMode = Schema.AccessMode.READ_ONLY)
    val updatedAt: LocalDateTime,
)
