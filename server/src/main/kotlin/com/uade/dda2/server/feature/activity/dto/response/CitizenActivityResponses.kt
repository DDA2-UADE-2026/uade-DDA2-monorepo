package com.uade.dda2.server.feature.activity.dto.response

import com.uade.dda2.server.feature.activity.entity.ActivityAttendanceStatus
import com.uade.dda2.server.feature.activity.entity.ActivityStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Schema(description = "Actividad disponible para la comunidad o incluida en las inscripciones del ciudadano autenticado.")
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
    val status: ActivityStatus,
    val myEnrollment: CitizenActivityEnrollmentResponse?,
)

@Schema(description = "Inscripción del ciudadano autenticado, sin datos de otros participantes.")
data class CitizenActivityEnrollmentResponse(
    val id: UUID,
    val enrolledAt: OffsetDateTime,
    val attendance: ActivityAttendanceStatus?,
)

@Schema(description = "Página de actividades abiertas o de inscripciones propias, incluidas las actividades cerradas.")
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
