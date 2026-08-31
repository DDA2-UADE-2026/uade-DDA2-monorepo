package com.uade.dda2.server.feature.application.dto.response

import com.uade.dda2.server.feature.application.entity.ApplicationStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.util.UUID

@Schema(description = "Solicitud del usuario autenticado. No expone datos internos de idempotencia ni de otros usuarios.")
data class ApplicationResponse(
    val id: UUID,
    @field:Schema(description = "Número global único, generado por secuencia. Puede haber saltos.", example = "15432")
    val applicationNumber: Long,
    val programEditionId: UUID,
    val enrollmentPeriodId: UUID,
    val status: ApplicationStatus,
    val submittedAt: LocalDateTime,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)

data class ApplicationListResponse(
    val content: List<ApplicationResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)
