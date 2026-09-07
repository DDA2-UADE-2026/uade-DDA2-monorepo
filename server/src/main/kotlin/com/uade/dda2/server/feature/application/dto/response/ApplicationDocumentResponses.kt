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
