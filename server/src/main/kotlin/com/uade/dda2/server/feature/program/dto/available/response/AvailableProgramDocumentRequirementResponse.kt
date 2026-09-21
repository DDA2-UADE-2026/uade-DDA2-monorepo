package com.uade.dda2.server.feature.program.dto.available.response

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "Documento solicitado por una edición disponible.")
data class AvailableProgramDocumentRequirementResponse(
    val id: UUID,
    val code: String,
    val name: String,
    val description: String?,
    val required: Boolean,
)
