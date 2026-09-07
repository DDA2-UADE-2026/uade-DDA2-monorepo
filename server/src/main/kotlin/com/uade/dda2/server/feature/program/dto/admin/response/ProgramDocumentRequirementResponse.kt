package com.uade.dda2.server.feature.program.dto.admin.response

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "Documento que una edición solicita a sus postulantes.")
data class ProgramDocumentRequirementResponse(
    val id: UUID,
    val programEditionId: UUID,
    val code: String,
    val name: String,
    val description: String?,
    val required: Boolean,
)
