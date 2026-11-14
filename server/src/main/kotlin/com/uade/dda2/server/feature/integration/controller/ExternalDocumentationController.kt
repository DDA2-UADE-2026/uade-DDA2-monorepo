package com.uade.dda2.server.feature.integration.controller

import com.uade.dda2.server.feature.application.dto.request.ReviewApplicationDocumentRequest
import com.uade.dda2.server.feature.integration.dto.ExternalDocumentationResponse
import com.uade.dda2.server.feature.integration.service.ExternalDocumentationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import java.util.UUID
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/applications/{applicationId}/external-documents")
@Validated
@Tag(name = "Core — Documentación M1", description = "Referencias documentales externas. COMPLETED requiere revisión de M8 antes de quedar VALID.")
class ExternalDocumentationController(
    private val service: ExternalDocumentationService,
) {
    @PreAuthorize("hasAuthority('applications:management:documents:view')")
    @GetMapping
    @Operation(summary = "Consultar entregas documentales de M1")
    fun list(
        @PathVariable applicationId: UUID,
    ): List<ExternalDocumentationResponse> =
        service.list(applicationId)

    @PreAuthorize("hasAuthority('applications:management:documents:review')")
    @PatchMapping("/{requestId}/review")
    @Operation(summary = "Revisar una entrega externa", description = "VALID sin observación u OBSERVED con observación. No descarga documentos ni valida automáticamente su contenido.")
    fun review(
        @PathVariable applicationId: UUID,
        @PathVariable requestId: UUID,
        @Valid @RequestBody request: ReviewApplicationDocumentRequest,
    ): ExternalDocumentationResponse =
        service.review(applicationId, requestId, request)
}
