package com.uade.dda2.server.feature.application.dto.response

import com.uade.dda2.server.feature.application.entity.ApplicationVisitRescheduleRequestStatus
import com.uade.dda2.server.feature.application.entity.ApplicationVisitType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime
import java.util.UUID

@Schema(description = "Resumen sin textos sensibles de la solicitud de reprogramacion mas reciente.")
data class ApplicationVisitRescheduleRequestSummaryResponse(
    val id: UUID,
    val status: ApplicationVisitRescheduleRequestStatus,
    val requestedAt: OffsetDateTime,
    val reviewedAt: OffsetDateTime?,
)

@Schema(description = "Horario historico de una visita o entrevista.")
data class ApplicationVisitRescheduleScheduleResponse(
    val startsAt: OffsetDateTime,
    val endsAt: OffsetDateTime,
    val durationMinutes: Int,
    val location: String,
)

@Schema(description = "Solicitud propia de reprogramacion incluida en el historial ciudadano.")
data class CitizenApplicationVisitRescheduleRequestListItemResponse(
    val id: UUID,
    val applicationId: UUID,
    val applicationNumber: Long,
    val visitId: UUID,
    val visitType: ApplicationVisitType,
    val status: ApplicationVisitRescheduleRequestStatus,
    val requestedAt: OffsetDateTime,
    val reviewedAt: OffsetDateTime?,
    val approvedStartsAt: OffsetDateTime?,
    val approvedEndsAt: OffsetDateTime?,
    val approvedDurationMinutes: Int?,
)

@Schema(description = "Pagina del historial ciudadano de solicitudes de reprogramacion.")
data class CitizenApplicationVisitRescheduleRequestListResponse(
    val content: List<CitizenApplicationVisitRescheduleRequestListItemResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

@Schema(description = "Detalle ciudadano de una solicitud de reprogramacion propia.")
data class CitizenApplicationVisitRescheduleRequestDetailResponse(
    val id: UUID,
    val applicationId: UUID,
    val applicationNumber: Long,
    val visitId: UUID,
    val visitType: ApplicationVisitType,
    val status: ApplicationVisitRescheduleRequestStatus,
    val reason: String,
    val requestedAt: OffsetDateTime,
    val scheduleAtRequest: ApplicationVisitRescheduleScheduleResponse,
    val reviewedAt: OffsetDateTime?,
    val reviewComment: String?,
    val approvedSchedule: ApplicationVisitRescheduleScheduleResponse?,
)

@Schema(description = "Solicitud de reprogramacion incluida en la bandeja administrativa.")
data class AdminApplicationVisitRescheduleRequestListItemResponse(
    val id: UUID,
    val applicationId: UUID,
    val applicationNumber: Long,
    val visitId: UUID,
    val visitType: ApplicationVisitType,
    val applicantUserId: Long,
    val applicantName: String,
    val responsibleId: Long,
    val responsibleName: String,
    val programId: UUID,
    val programName: String,
    val programEditionId: UUID,
    val programEditionName: String,
    val status: ApplicationVisitRescheduleRequestStatus,
    val requestedAt: OffsetDateTime,
    val reviewedAt: OffsetDateTime?,
    val originalStartsAt: OffsetDateTime,
    val originalEndsAt: OffsetDateTime,
    val originalDurationMinutes: Int,
    val approvedStartsAt: OffsetDateTime?,
    val approvedEndsAt: OffsetDateTime?,
    val approvedDurationMinutes: Int?,
)

@Schema(description = "Pagina filtrada de solicitudes de reprogramacion.")
data class AdminApplicationVisitRescheduleRequestListResponse(
    val content: List<AdminApplicationVisitRescheduleRequestListItemResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

@Schema(description = "Detalle administrativo completo de una solicitud de reprogramacion.")
data class AdminApplicationVisitRescheduleRequestDetailResponse(
    val id: UUID,
    val applicationId: UUID,
    val applicationNumber: Long,
    val visitId: UUID,
    val visitType: ApplicationVisitType,
    val applicantUserId: Long,
    val applicantName: String,
    val applicantEmail: String,
    val responsibleId: Long,
    val responsibleName: String,
    val responsibleEmail: String,
    val programId: UUID,
    val programName: String,
    val programEditionId: UUID,
    val programEditionName: String,
    val requestedById: Long,
    val requestedByName: String,
    val status: ApplicationVisitRescheduleRequestStatus,
    val reason: String,
    val requestedAt: OffsetDateTime,
    val scheduleAtRequest: ApplicationVisitRescheduleScheduleResponse,
    val reviewedById: Long?,
    val reviewedByName: String?,
    val reviewedByEmail: String?,
    val reviewedAt: OffsetDateTime?,
    val reviewComment: String?,
    val approvedSchedule: ApplicationVisitRescheduleScheduleResponse?,
)
