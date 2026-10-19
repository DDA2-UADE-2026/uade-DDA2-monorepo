package com.uade.dda2.server.feature.application.dto.response

import com.uade.dda2.server.feature.application.entity.ApplicationResolutionDecision
import com.uade.dda2.server.feature.application.entity.ApplicationStatus
import com.uade.dda2.server.feature.program.entity.enums.ProgramRequirementType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime
import java.util.UUID

@Schema(description = "Resolucion historica inmutable, visible solo para administracion.")
data class ApplicationResolutionResponse(
    val id: UUID,
    val applicationId: UUID,
    val previousStatus: ApplicationStatus,
    val decision: ApplicationResolutionDecision,
    val reason: String,
    val resolvedByUserId: Long,
    val resolvedByUserName: String,
    @field:Schema(description = "Fecha de resolucion en UTC.")
    val resolvedAt: OffsetDateTime,
    val requirementChecks: List<ApplicationResolutionRequirementResponse>,
)

@Schema(description = "Copia del requisito y resultado al momento de resolver; no sigue los cambios del catalogo.")
data class ApplicationResolutionRequirementResponse(
    val requirementId: UUID,
    val type: ProgramRequirementType,
    val value: String,
    val description: String?,
    val fulfilled: Boolean,
)
