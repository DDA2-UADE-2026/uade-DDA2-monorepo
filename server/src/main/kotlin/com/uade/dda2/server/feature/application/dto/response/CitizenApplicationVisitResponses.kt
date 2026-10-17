package com.uade.dda2.server.feature.application.dto.response

import com.uade.dda2.server.feature.application.entity.ApplicationVisitStatus
import com.uade.dda2.server.feature.application.entity.ApplicationVisitType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime
import java.util.UUID

@Schema(description = "Resumen ciudadano de una visita o entrevista asociada a una solicitud propia.")
data class CitizenApplicationVisitListItemResponse(
    val id: UUID,
    val applicationId: UUID,
    val applicationNumber: Long,
    @field:Schema(description = "Modalidad de la actuacion: VISIT o INTERVIEW.", example = "VISIT")
    val type: ApplicationVisitType,
    @field:Schema(description = "Inicio vigente expresado en la zona horaria municipal.", example = "2026-10-20T10:00:00-03:00")
    val startsAt: OffsetDateTime,
    @field:Schema(description = "Fin vigente expresado en la zona horaria municipal.", example = "2026-10-20T11:00:00-03:00")
    val endsAt: OffsetDateTime,
    val durationMinutes: Int,
    val location: String,
    val status: ApplicationVisitStatus,
    @field:Schema(description = "Indica si el horario o lugar fue reprogramado al menos una vez.")
    val rescheduled: Boolean,
    @field:Schema(description = "Instante UTC de la ultima reprogramacion.", nullable = true)
    val lastRescheduledAt: OffsetDateTime?,
    @field:Schema(description = "Instante UTC de cancelacion.", nullable = true)
    val cancelledAt: OffsetDateTime?,
)

@Schema(description = "Pagina de visitas y entrevistas de una solicitud propia.")
data class CitizenApplicationVisitListResponse(
    val content: List<CitizenApplicationVisitListItemResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

@Schema(description = "Detalle ciudadano de una visita o entrevista asociada a una solicitud propia.")
data class CitizenApplicationVisitDetailResponse(
    val id: UUID,
    val applicationId: UUID,
    val applicationNumber: Long,
    @field:Schema(description = "Modalidad de la actuacion: VISIT o INTERVIEW.", example = "VISIT")
    val type: ApplicationVisitType,
    @field:Schema(description = "Inicio vigente expresado en la zona horaria municipal.", example = "2026-10-20T10:00:00-03:00")
    val startsAt: OffsetDateTime,
    @field:Schema(description = "Fin vigente expresado en la zona horaria municipal.", example = "2026-10-20T11:00:00-03:00")
    val endsAt: OffsetDateTime,
    val durationMinutes: Int,
    val location: String,
    val status: ApplicationVisitStatus,
    @field:Schema(description = "Indica si el horario o lugar fue reprogramado al menos una vez.")
    val rescheduled: Boolean,
    @field:Schema(description = "Instante UTC de la ultima reprogramacion.", nullable = true)
    val lastRescheduledAt: OffsetDateTime?,
    @field:Schema(description = "Instante UTC de cancelacion.", nullable = true)
    val cancelledAt: OffsetDateTime?,
    @field:Schema(
        description = "Motivo de cancelacion visible solo para el titular en el detalle.",
        nullable = true,
    )
    val cancellationReason: String?,
)
