package com.uade.dda2.server.feature.intervention.dto.response

import com.uade.dda2.server.feature.intervention.entity.InterventionActionStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Schema(description = "Cabecera propia sin responsables. Estado calculado a partir de todas las acciones.")
data class CitizenInterventionPlanSummaryResponse(
    val id: UUID, val applicationId: UUID, val applicationNumber: Long,
    val programId: UUID, val programName: String, val programEditionId: UUID, val programEditionName: String,
    val createdAt: OffsetDateTime, val status: InterventionActionStatus,
)

@Schema(description = "Plan propio con estructura acordada y estado calculado; sin informacion profesional interna.")
data class CitizenInterventionPlanResponse(
    val id: UUID, val applicationId: UUID, val applicationNumber: Long,
    val programId: UUID, val programName: String, val programEditionId: UUID, val programEditionName: String,
    val createdAt: OffsetDateTime, val status: InterventionActionStatus,
    val objectives: List<CitizenInterventionObjectiveResponse>,
)

data class CitizenInterventionObjectiveResponse(val id: UUID, val description: String, val actions: List<CitizenInterventionActionResponse>)
data class CitizenInterventionActionResponse(
    val id: UUID, val description: String, val startDate: LocalDate, val dueDate: LocalDate, val status: InterventionActionStatus,
)

@Schema(description = "Unicamente la version publica vigente del seguimiento. No incluye secuencia interna, notas, resultado profesional ni autores.")
data class CitizenInterventionFollowUpResponse(
    val id: UUID, val actionId: UUID, val occurredOn: LocalDate, val status: InterventionActionStatus,
    val summary: String, val publishedAt: OffsetDateTime,
)

data class CitizenInterventionPlanListResponse(
    val content: List<CitizenInterventionPlanSummaryResponse>, val page: Int, val size: Int, val totalElements: Long, val totalPages: Int,
    @field:Schema(types = ["string", "null"], description = "Mensaje informativo solo cuando el total es cero.") val message: String?,
)
data class CitizenInterventionFollowUpListResponse(
    val content: List<CitizenInterventionFollowUpResponse>, val page: Int, val size: Int, val totalElements: Long, val totalPages: Int,
    @field:Schema(types = ["string", "null"], description = "Se cuentan solo publicaciones visibles; null si existen, incluso en paginas fuera de rango.") val message: String?,
)

@Schema(description = "Gestion profesional de la version publica actual, incluidas publicaciones retiradas.")
data class InterventionPublicationResponse(
    val id: UUID, val summary: String, val visible: Boolean, val publishedAt: OffsetDateTime, val updatedAt: OffsetDateTime,
    val updatedByUserId: Long, val updatedByUserName: String,
)
