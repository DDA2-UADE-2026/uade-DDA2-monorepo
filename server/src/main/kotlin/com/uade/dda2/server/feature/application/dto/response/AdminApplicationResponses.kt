package com.uade.dda2.server.feature.application.dto.response

import com.uade.dda2.server.feature.application.entity.ApplicationStatus
import com.uade.dda2.server.feature.program.dto.available.response.AvailableProgramDocumentRequirementResponse
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.util.UUID

@Schema(description = "Resumen de una solicitud en el listado administrativo. No incluye documentos ni campos internos de idempotencia.")
data class AdminApplicationListItemResponse(
    val id: UUID,
    @field:Schema(description = "Número global único, generado por secuencia. Puede haber saltos.", example = "15432")
    val applicationNumber: Long,
    @field:Schema(description = "ID interno del usuario titular de la solicitud.")
    val userId: Long,
    @field:Schema(description = "Nombre del titular registrado en users, no en una identidad externa.", example = "Ana Pérez")
    val userName: String,
    val userEmail: String,
    @field:Schema(description = "ID interno de quien registró la solicitud. En una presentación propia coincide con userId.")
    val registeredByUserId: Long,
    val programId: UUID,
    val programName: String,
    val programEditionId: UUID,
    val programEditionName: String,
    val enrollmentPeriodId: UUID,
    val status: ApplicationStatus,
    @field:Schema(description = "Trabajador asignado a la solicitud. Todavía no se asigna desde ninguna operación.", nullable = true)
    val assignedWorkerUserId: Long?,
    val submittedAt: LocalDateTime,
    @field:Schema(nullable = true)
    val resolvedAt: LocalDateTime?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)

@Schema(description = "Página de solicitudes de todos los titulares, ordenadas por número descendente.")
data class AdminApplicationListResponse(
    val content: List<AdminApplicationListItemResponse>,
    @field:Schema(description = "Número de página, comenzando en cero.", example = "0")
    val page: Int,
    @field:Schema(description = "Cantidad máxima de elementos por página.", example = "20")
    val size: Int,
    @field:Schema(description = "Cantidad total de solicitudes.", example = "42")
    val totalElements: Long,
    @field:Schema(description = "Cantidad total de páginas.", example = "3")
    val totalPages: Int,
)

@Schema(description = "Solicitud completa para administración: todos los campos de la entidad, incluidos los que la vista propia no expone.")
data class AdminApplicationResponse(
    val id: UUID,
    @field:Schema(description = "Número global único, generado por secuencia. Puede haber saltos.", example = "15432")
    val applicationNumber: Long,
    @field:Schema(description = "ID interno del usuario titular de la solicitud.")
    val userId: Long,
    @field:Schema(description = "Nombre del titular registrado en users, no en una identidad externa.", example = "Ana Pérez")
    val userName: String,
    val userEmail: String,
    @field:Schema(description = "ID interno de quien registró la solicitud. En una presentación propia coincide con userId.")
    val registeredByUserId: Long,
    val registeredByUserName: String,
    val programId: UUID,
    val programName: String,
    val programEditionId: UUID,
    val programEditionName: String,
    val enrollmentPeriodId: UUID,
    val status: ApplicationStatus,
    @field:Schema(description = "Identificador del trámite de origen, cuando la solicitud llegó desde otro canal.", nullable = true)
    val originTicketId: String?,
    @field:Schema(description = "Motivo de la resolución. Todavía no se completa desde ninguna operación.", nullable = true)
    val resolutionReason: String?,
    @field:Schema(description = "Trabajador asignado a la solicitud. Todavía no se asigna desde ninguna operación.", nullable = true)
    val assignedWorkerUserId: Long?,
    @field:Schema(nullable = true)
    val assignedWorkerName: String?,
    val submittedAt: LocalDateTime,
    @field:Schema(nullable = true)
    val resolvedAt: LocalDateTime?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    @field:Schema(description = "Clave de idempotencia con la que se presentó la solicitud, solo visible en esta vista administrativa.", nullable = true)
    val idempotencyKey: String?,
    @field:Schema(description = "Hash del cuerpo asociado a la clave de idempotencia. Existe si y solo si existe la clave.", nullable = true)
    val requestHash: String?,
    @field:Schema(description = "Documentos obligatorios todavía faltantes u observados. Una entrega PENDING ya presentada no aparece aquí.")
    val pendingDocuments: List<PendingApplicationDocumentResponse>,
    @field:Schema(description = "Catálogo completo de documentos de la edición, incluidos los opcionales, aunque la edición ya no esté disponible para nuevas solicitudes.")
    val documentRequirements: List<AvailableProgramDocumentRequirementResponse>,
)
