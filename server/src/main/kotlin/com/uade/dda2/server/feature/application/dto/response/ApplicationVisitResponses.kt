package com.uade.dda2.server.feature.application.dto.response

import com.uade.dda2.server.feature.application.entity.ApplicationStatus
import com.uade.dda2.server.feature.application.entity.ApplicationVisitStatus
import com.uade.dda2.server.feature.application.entity.ApplicationVisitType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime
import java.util.UUID

@Schema(description = "Estado vigente de una visita o entrevista social programada, cancelada o finalizada.")
data class ApplicationVisitResponse(
    val id: UUID,
    val applicationId: UUID,
    val applicationStatus: ApplicationStatus,
    val type: ApplicationVisitType,
    val startsAt: OffsetDateTime,
    val endsAt: OffsetDateTime,
    val durationMinutes: Int,
    val location: String,
    val status: ApplicationVisitStatus,
    val responsibleId: Long,
    val responsibleName: String,
    @field:Schema(description = "Instante UTC asignado por el servidor.", example = "2026-10-14T18:30:00Z")
    val scheduledAt: OffsetDateTime,
    @field:Schema(description = "Motivo de cancelacion; null mientras la actuacion siga programada.", nullable = true)
    val cancellationReason: String?,
    @field:Schema(description = "Instante UTC de cancelacion.", nullable = true)
    val cancelledAt: OffsetDateTime?,
    val cancelledById: Long?,
    val cancelledByName: String?,
    @field:Schema(description = "Observaciones del resultado; null mientras la actuacion no haya finalizado.", nullable = true)
    val observations: String?,
    @field:Schema(description = "Conclusiones del resultado; null mientras la actuacion no haya finalizado.", nullable = true)
    val conclusions: String?,
    @field:Schema(description = "Instante UTC en que se registro el resultado.", nullable = true)
    val resultRecordedAt: OffsetDateTime?,
    val resultRecordedById: Long?,
    val resultRecordedByName: String?,
)

@Schema(description = "Actuacion incluida en la agenda administrativa de solicitudes.")
data class ApplicationVisitListItemResponse(
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
    val type: ApplicationVisitType,
    val startsAt: OffsetDateTime,
    val endsAt: OffsetDateTime,
    val durationMinutes: Int,
    val location: String,
    val status: ApplicationVisitStatus,
    val responsibleId: Long,
    val responsibleName: String,
    val scheduledAt: OffsetDateTime,
    val cancelledAt: OffsetDateTime?,
    val cancelledById: Long?,
    val cancelledByName: String?,
    val resultRecordedAt: OffsetDateTime?,
    val resultRecordedById: Long?,
    val resultRecordedByName: String?,
)

@Schema(description = "Pagina de visitas y entrevistas sociales ordenada cronologicamente.")
data class ApplicationVisitListResponse(
    val content: List<ApplicationVisitListItemResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

@Schema(description = "Detalle administrativo de una visita o entrevista y su solicitud asociada.")
data class ApplicationVisitDetailResponse(
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
    val type: ApplicationVisitType,
    val startsAt: OffsetDateTime,
    val endsAt: OffsetDateTime,
    val durationMinutes: Int,
    val location: String,
    val status: ApplicationVisitStatus,
    val responsibleId: Long,
    val responsibleName: String,
    val responsibleEmail: String,
    val scheduledAt: OffsetDateTime,
    @field:Schema(description = "Motivo de cancelacion; no se expone en el listado ni en auditoria.", nullable = true)
    val cancellationReason: String?,
    val cancelledAt: OffsetDateTime?,
    val cancelledById: Long?,
    val cancelledByName: String?,
    val cancelledByEmail: String?,
    @field:Schema(description = "Observaciones sensibles del resultado, visibles solo en el detalle autorizado.", nullable = true)
    val observations: String?,
    @field:Schema(description = "Conclusiones sensibles del resultado, visibles solo en el detalle autorizado.", nullable = true)
    val conclusions: String?,
    val resultRecordedAt: OffsetDateTime?,
    val resultRecordedById: Long?,
    val resultRecordedByName: String?,
    val resultRecordedByEmail: String?,
)
