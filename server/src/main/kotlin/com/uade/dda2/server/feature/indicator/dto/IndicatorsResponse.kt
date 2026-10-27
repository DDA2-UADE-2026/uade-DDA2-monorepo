package com.uade.dda2.server.feature.indicator.dto

import com.uade.dda2.server.feature.application.entity.ApplicationStatus
import com.uade.dda2.server.feature.application.entity.EvaluationPriority
import com.uade.dda2.server.feature.application.entity.VulnerabilityLevel
import com.uade.dda2.server.feature.program.entity.enums.ProgramEditionStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.UUID

enum class IndicatorBucket { WEEK, MONTH }

@Schema(description = "Tablero de indicadores del módulo, agregados en el servidor para un período y, opcionalmente, un programa.")
data class IndicatorsResponse(
    @field:Schema(description = "Inicio del período, inclusive.") val from: LocalDate,
    @field:Schema(description = "Fin del período, inclusive.") val to: LocalDate,
    @field:Schema(nullable = true) val programId: UUID?,
    val generatedAt: OffsetDateTime,
    @field:Schema(description = "Agrupación de las series temporales: semanas (lunes) para períodos de hasta 120 días, meses si no.")
    val bucket: IndicatorBucket,
    val summary: SummaryIndicators,
    @field:Schema(description = "Solicitudes presentadas y resueltas por intervalo.")
    val applicationsTrend: List<ApplicationTrendPoint>,
    @field:Schema(description = "Estado actual de las solicitudes presentadas en el período.")
    val statusDistribution: List<StatusCount>,
    @field:Schema(description = "Resoluciones registradas en el período, por resultado.")
    val resolutions: ResolutionCounts,
    @field:Schema(description = "Solicitudes presentadas en el período por programa y resultado actual.")
    val programs: List<ProgramIndicator>,
    @field:Schema(description = "Ocupación actual del cupo de cada edición no borrador; no depende del período.")
    val capacity: List<EditionCapacity>,
    @field:Schema(description = "Evaluaciones registradas en el período, por vulnerabilidad y prioridad.")
    val evaluationMatrix: List<EvaluationMatrixCell>,
    @field:Schema(description = "Casos que necesitan atención hoy; no depende del período.")
    val attention: AttentionIndicators,
    @field:Schema(description = "Visitas y entrevistas con inicio en el período, por intervalo y estado.")
    val visitsTrend: List<VisitTrendPoint>,
    @field:Schema(description = "Pedidos de reprogramación presentados en el período.")
    val rescheduleRequests: RescheduleRequestCounts,
    @field:Schema(description = "Casos activos y visitas programadas por trabajador asignado; no depende del período.")
    val workload: List<WorkerLoad>,
    @field:Schema(description = "Días promedio entre etapas de las solicitudes resueltas en el período.")
    val stageTimes: StageTimes,
    @field:Schema(description = "Documentos subidos en el período, por estado de revisión.")
    val documents: DocumentCounts,
    @field:Schema(description = "Turnos de salud comunitaria con inicio en el período. No se filtra por programa.")
    val appointments: AppointmentIndicators,
    @field:Schema(description = "Inscripciones a actividades registradas en el período. No se filtra por programa.")
    val activities: ActivityIndicators,
)

data class SummaryIndicators(
    val applicationsSubmitted: Long,
    @field:Schema(description = "Presentadas en el período anterior de igual duración.")
    val previousApplicationsSubmitted: Long,
    val resolved: Long,
    @field:Schema(description = "Aprobadas sobre aprobadas más rechazadas, entre las resueltas en el período. Null si no hubo.", nullable = true)
    val approvalRate: Double?,
    @field:Schema(description = "Aprobadas sobre el cupo total de las ediciones consideradas. Null si no hay cupo.", nullable = true)
    val capacityUsedRate: Double?,
    val approvedTotal: Long,
    val capacityTotal: Long,
    val visitsRealized: Long,
    val previousVisitsRealized: Long,
    @field:Schema(description = "Días promedio desde la presentación hasta la resolución. Null si no hubo resoluciones.", nullable = true)
    val averageDaysToResolution: Double?,
)

data class ApplicationTrendPoint(val periodStart: LocalDate, val submitted: Long, val resolved: Long)

data class StatusCount(val status: ApplicationStatus, val count: Long)

data class ResolutionCounts(val approved: Long, val waitlisted: Long, val rejected: Long)

data class ProgramIndicator(
    val programId: UUID,
    val programName: String,
    val submitted: Long,
    @field:Schema(description = "Todavía sin resolución definitiva ni lista de espera.")
    val inProgress: Long,
    val approved: Long,
    val waitlisted: Long,
    val rejected: Long,
)

data class EditionCapacity(
    val editionId: UUID,
    val programId: UUID,
    val programName: String,
    val editionName: String,
    val status: ProgramEditionStatus,
    val maxCapacity: Int,
    val approved: Long,
    val waitlisted: Long,
)

data class EvaluationMatrixCell(val vulnerability: VulnerabilityLevel, val priority: EvaluationPriority, val count: Long)

data class AttentionIndicators(
    @field:Schema(description = "Solicitudes activas sin trabajador asignado.")
    val unassigned: Long,
    @field:Schema(description = "Solicitudes sin evaluar con más de staleAfterDays días desde su presentación.")
    val awaitingEvaluation: Long,
    val staleAfterDays: Int,
    @field:Schema(description = "Las solicitudes sin evaluar más antiguas, hasta cinco.")
    val oldestAwaitingEvaluation: List<AwaitingApplication>,
)

data class AwaitingApplication(
    val applicationId: UUID,
    val applicationNumber: Long,
    val programName: String,
    val status: ApplicationStatus,
    val submittedAt: LocalDateTime,
    val daysWaiting: Long,
)

data class VisitTrendPoint(
    val periodStart: LocalDate,
    val scheduled: Long,
    val realized: Long,
    val notRealized: Long,
    val cancelled: Long,
)

data class RescheduleRequestCounts(val pending: Long, val approved: Long, val rejected: Long)

data class WorkerLoad(val userId: Long, val name: String, val activeCases: Long, val scheduledVisits: Long)

data class StageTimes(
    @field:Schema(nullable = true) val averageDaysToEvaluation: Double?,
    @field:Schema(nullable = true) val averageDaysEvaluationToResolution: Double?,
    @field:Schema(nullable = true) val averageDaysToResolution: Double?,
    val sampleSize: Long,
)

data class DocumentCounts(val pending: Long, val valid: Long, val observed: Long)

data class AppointmentIndicators(
    val confirmed: Long,
    val cancelled: Long,
    val attended: Long,
    val absent: Long,
    val trend: List<AppointmentTrendPoint>,
)

data class AppointmentTrendPoint(val periodStart: LocalDate, val confirmed: Long, val cancelled: Long)

data class ActivityIndicators(
    val enrollments: Long,
    val present: Long,
    val absent: Long,
    @field:Schema(description = "Inscripciones todavía sin asistencia registrada.")
    val unrecorded: Long,
)
