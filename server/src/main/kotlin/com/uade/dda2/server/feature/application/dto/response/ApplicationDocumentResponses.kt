package com.uade.dda2.server.feature.application.dto.response

import com.uade.dda2.server.feature.application.entity.ApplicationDocumentStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.util.UUID

enum class PendingDocumentReason { MISSING, OBSERVED }

data class PendingApplicationDocumentResponse(
    val requirementId: UUID,
    val code: String,
    val name: String,
    val reason: PendingDocumentReason,
    val observation: String?,
)

@Schema(description = "Metadatos de un documento entregado; nunca contiene sus bytes.")
data class ApplicationDocumentResponse(
    val id: UUID,
    val applicationId: UUID,
    val requirementId: UUID,
    val requirementCode: String,
    val requirementName: String,
    val required: Boolean,
    val documentId: UUID,
    val originalName: String,
    val contentType: String,
    val sizeBytes: Long,
    val status: ApplicationDocumentStatus,
    val observation: String?,
    val uploadedAt: LocalDateTime,
    val reviewedByUserId: Long?,
    @field:Schema(description = "Nombre de quien revisó la entrega; sólo se expone en las vistas administrativas.", nullable = true)
    val reviewedByUserName: String?,
    @field:Schema(description = "Correo de quien revisó la entrega; sólo se expone en las vistas administrativas.", nullable = true)
    val reviewedByUserEmail: String?,
    val reviewedAt: LocalDateTime?,
    val contentUrl: String,
)

data class ApplicationDocumentContent(
    val originalName: String,
    val contentType: String,
    val sizeBytes: Long,
    val content: ByteArray,
)

data class ApplicationDocumentMutation(
    val document: ApplicationDocumentResponse,
    val created: Boolean,
)
