package com.uade.dda2.server.feature.application.dto.response

import com.uade.dda2.server.feature.application.entity.EvaluationPriority
import com.uade.dda2.server.feature.application.entity.ApplicationStatus
import com.uade.dda2.server.feature.application.entity.VulnerabilityLevel
import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime
import java.util.UUID

@Schema(description = "Evaluacion social registrada para una solicitud.")
data class ApplicationEvaluationResponse(
    val id: UUID,
    val applicationId: UUID,
    val vulnerability: VulnerabilityLevel,
    val priority: EvaluationPriority,
    val recommendation: String,
    @field:Schema(description = "Usuario autenticado que registro la evaluacion.")
    val professionalId: Long,
    val professionalName: String,
    @field:Schema(description = "Instante UTC asignado por el servidor.", example = "2026-10-10T18:30:00Z")
    val evaluatedAt: OffsetDateTime,
)

@Schema(description = "Resumen administrativo de una evaluacion social. La recomendacion se consulta solo en el detalle.")
data class ApplicationEvaluationListItemResponse(
    val id: UUID,
    val applicationId: UUID,
    val applicationNumber: Long,
    val applicationStatus: ApplicationStatus,
    val applicantUserId: Long,
    val applicantName: String,
    val programId: UUID,
    val programName: String,
    val programEditionId: UUID,
    val programEditionName: String,
    val vulnerability: VulnerabilityLevel,
    val priority: EvaluationPriority,
    val professionalId: Long,
    val professionalName: String,
    val evaluatedAt: OffsetDateTime,
    @field:Schema(description = "Plan de intervencion registrado para la solicitud, si existe.")
    val interventionPlanId: UUID?,
    @field:Schema(description = "Cantidad total de acciones del plan de intervencion.", minimum = "0")
    val interventionActionCount: Long,
    @field:Schema(description = "Cantidad de acciones completadas del plan de intervencion.", minimum = "0")
    val interventionCompletedActionCount: Long,
)

@Schema(description = "Pagina administrativa de evaluaciones sociales, ordenada por fecha descendente.")
data class ApplicationEvaluationListResponse(
    val content: List<ApplicationEvaluationListItemResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

@Schema(description = "Detalle administrativo completo de una evaluacion social y su solicitud asociada.")
data class ApplicationEvaluationDetailResponse(
    val id: UUID,
    val applicationId: UUID,
    val applicationNumber: Long,
    val applicationStatus: ApplicationStatus,
    val applicantUserId: Long,
    val applicantName: String,
    val applicantEmail: String,
    val programId: UUID,
    val programName: String,
    val programEditionId: UUID,
    val programEditionName: String,
    val enrollmentPeriodId: UUID,
    val vulnerability: VulnerabilityLevel,
    val priority: EvaluationPriority,
    @field:Schema(description = "Recomendacion profesional sensible, visible solo en el detalle autorizado.")
    val recommendation: String,
    val professionalId: Long,
    val professionalName: String,
    val professionalEmail: String,
    @field:Schema(description = "Instante UTC asignado por el servidor.", example = "2026-10-10T18:30:00Z")
    val evaluatedAt: OffsetDateTime,
)
