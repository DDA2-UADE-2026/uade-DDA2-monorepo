package com.uade.dda2.server.feature.center.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.OffsetDateTime
import java.util.UUID

@Schema(description = "Disponibilidad semanal para una asignación profesional y servicio.")
data class ProfessionalAvailabilityResponse(
    val id: UUID,
    val assignmentId: UUID,
    val professionalId: Long,
    val centerId: UUID,
    val serviceId: UUID,
    val serviceName: String,
    val dayOfWeek: DayOfWeek,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val active: Boolean,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)
