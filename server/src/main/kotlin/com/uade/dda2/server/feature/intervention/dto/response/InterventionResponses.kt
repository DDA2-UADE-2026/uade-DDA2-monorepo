package com.uade.dda2.server.feature.intervention.dto.response

import com.uade.dda2.server.feature.intervention.entity.InterventionActionStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Schema(description = "Plan profesional de un caso. Estructura inicial fija y estados actuales de sus acciones.")
data class InterventionPlanResponse(
    val id: UUID,
    val applicationId: UUID,
    val userId: Long,
    val responsibleUserId: Long,
    val responsibleUserName: String,
    @field:Schema(description = "Instante UTC de creacion, con precision de microsegundos.")
    val createdAt: OffsetDateTime,
    val objectives: List<InterventionObjectiveResponse>,
)
data class InterventionObjectiveResponse(val id: UUID, val description: String, val actions: List<InterventionActionResponse>)
data class InterventionActionResponse(
    val id: UUID, val description: String, val startDate: LocalDate, val dueDate: LocalDate,
    val status: InterventionActionStatus, val responsibleUserId: Long, val responsibleUserName: String,
)
@Schema(description = "Seguimiento profesional inmutable; los textos no se exponen a ciudadanos ni a logs generales.")
data class InterventionFollowUpResponse(
    val id: UUID, val planId: UUID, val applicationId: UUID, val userId: Long, val objectiveId: UUID, val actionId: UUID,
    val sequenceNumber: Int, val previousStatus: InterventionActionStatus, val status: InterventionActionStatus,
    val occurredOn: LocalDate, val notes: String, val result: String?,
    val authorUserId: Long, val authorUserName: String,
    @field:Schema(description = "Instante UTC de registro. Puede ser posterior a la fecha real de una carga historica.")
    val recordedAt: OffsetDateTime,
)
