package com.uade.dda2.server.feature.application.dto.request

import com.uade.dda2.server.feature.application.entity.ApplicationDocumentStatus
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

@Schema(description = "Resultado de la revisión administrativa de un documento pendiente.")
data class ReviewApplicationDocumentRequest(
    @field:Schema(allowableValues = ["VALID", "OBSERVED"])
    val status: ApplicationDocumentStatus,
    @field:Size(max = 1000)
    val observation: String? = null,
)
