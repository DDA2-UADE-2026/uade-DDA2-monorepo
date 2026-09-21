package com.uade.dda2.server.feature.application.dto.response

import com.uade.dda2.server.feature.application.entity.ApplicationStatus
import com.uade.dda2.server.feature.program.dto.available.response.AvailableProgramDocumentRequirementResponse
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
    @field:Schema(description = "Documentos obligatorios todavía faltantes u observados. Una entrega PENDING ya presentada no aparece aquí.")
    val pendingDocuments: List<PendingApplicationDocumentResponse>,
    val programId: UUID,
    val programName: String,
    val programEditionName: String,
    @field:Schema(description = "Catálogo completo de documentos de la edición, incluidos los opcionales, aunque la edición ya no esté disponible para nuevas solicitudes.")
    val documentRequirements: List<AvailableProgramDocumentRequirementResponse>,
)

data class ApplicationListResponse(
    val content: List<ApplicationResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)
