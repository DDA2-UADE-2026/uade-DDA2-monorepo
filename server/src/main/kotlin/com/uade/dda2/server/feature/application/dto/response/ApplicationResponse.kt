package com.uade.dda2.server.feature.application.dto.response

import com.uade.dda2.server.feature.application.entity.ApplicationStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.util.UUID

@Schema(description = "Solicitud y referencias a su titular y registrante. No expone datos personales ni internos de idempotencia.")
data class ApplicationResponse(
    val id: UUID,
    @field:Schema(description = "Número global único, generado por secuencia. Puede haber saltos.", example = "15432")
    val applicationNumber: Long,
    @field:Schema(description = "ID interno del usuario titular de la solicitud.")
    val userId: Long,
    @field:Schema(description = "ID interno de quien registró la solicitud, obtenido del JWT. En una presentación propia coincide con userId.")
    val registeredByUserId: Long,
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
