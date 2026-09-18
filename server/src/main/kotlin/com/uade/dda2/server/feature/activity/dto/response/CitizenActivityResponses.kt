package com.uade.dda2.server.feature.activity.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Schema(description = "Actividad abierta disponible para la comunidad.")
data class CitizenActivityResponse(
    val id: UUID,
    val name: String,
    val description: String,
    val location: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val capacity: Int,
    val enrolledCount: Long,
    val availableCapacity: Long,
)

@Schema(description = "Página de actividades abiertas disponibles para la comunidad.")
data class CitizenActivityListResponse(
    val content: List<CitizenActivityResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

@Schema(description = "Confirmación de inscripción a una actividad.")
data class ActivityEnrollmentResponse(
    val id: UUID,
    val activityId: UUID,
    val citizenId: Long,
    val enrolledAt: OffsetDateTime,
    val confirmation: String = "CONFIRMED",
)
