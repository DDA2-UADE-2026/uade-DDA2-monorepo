package com.uade.dda2.server.feature.activity.dto.response

import com.uade.dda2.server.feature.activity.entity.ActivityAttendanceStatus
import com.uade.dda2.server.feature.activity.entity.ActivityStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Schema(description = "Actividad disponible para el registro profesional de asistencia.")
data class ProfessionalActivityResponse(
    val id: UUID,
    val name: String,
    val location: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val status: ActivityStatus,
    val enrolledCount: Long,
)

data class ProfessionalActivityListResponse(
    val content: List<ProfessionalActivityResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

@Schema(description = "Usuario que registró la asistencia.")
data class AttendanceRecordedByResponse(
    val id: Long,
    val name: String,
)

@Schema(description = "Inscripción visible para un profesional de centro.")
data class ProfessionalActivityEnrollmentResponse(
    val id: UUID,
    val activityId: UUID,
    val citizenId: Long,
    val citizenName: String,
    val enrolledAt: OffsetDateTime,
    val attendance: ActivityAttendanceStatus?,
    val attendanceRecordedBy: AttendanceRecordedByResponse?,
    val attendanceRecordedAt: OffsetDateTime?,
)

data class ProfessionalActivityEnrollmentListResponse(
    val content: List<ProfessionalActivityEnrollmentResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)
