package com.uade.dda2.server.feature.integration.controller

import com.uade.dda2.server.feature.application.dto.request.ReviewApplicationDocumentRequest
import com.uade.dda2.server.feature.integration.dto.ExternalDocumentationResponse
import com.uade.dda2.server.feature.integration.service.ExternalDocumentationService
import com.uade.dda2.server.feature.integration.service.ExternalDocumentFileService
import com.uade.dda2.server.error.ErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import java.util.UUID
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
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
    private val files: ExternalDocumentFileService,
) {
    @PreAuthorize("hasAuthority('applications:management:documents:view')")
    @GetMapping
    @Operation(summary = "Consultar entregas documentales de M1")
    fun list(
        @PathVariable applicationId: UUID,
    ): List<ExternalDocumentationResponse> =
        service.list(applicationId)

    @PreAuthorize("hasAuthority('applications:management:documents:view')")
    @GetMapping("/{requestId}/file")
    @Operation(summary = "Descargar el archivo externo mediante M8", description = "Resuelve el documentId persistido de una entrega COMPLETED y descarga con una credencial de servicio. Requiere acordar y configurar el acceso por UUID en M1; desactivado por defecto. Hasta 10 MiB, sin caché, como adjunto y sin reenviar credenciales al navegador.")
    @ApiResponse(responseCode = "200", description = "Archivo como adjunto (PDF, PNG, JPEG u octet-stream).", content = [Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE, schema = Schema(type = "string", format = "binary"))])
    @ApiResponse(responseCode = "409", description = "La referencia todavía no tiene un archivo COMPLETED.", content = [Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = Schema(implementation = ErrorResponse::class))])
    @ApiResponse(responseCode = "502", description = "Acceso de servicio rechazado, respuesta inválida, archivo mayor al límite o error de M1.", content = [Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = Schema(implementation = ErrorResponse::class))])
    @ApiResponse(responseCode = "503", description = "Proxy deshabilitado o M1 no disponible.", content = [Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = Schema(implementation = ErrorResponse::class))])
    @ApiResponse(responseCode = "504", description = "M1 excedió el plazo de descarga.", content = [Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = Schema(implementation = ErrorResponse::class))])
    fun file(@PathVariable applicationId: UUID, @PathVariable requestId: UUID): ResponseEntity<ByteArray> {
        val file = files.download(applicationId, requestId)
        return ResponseEntity.ok().contentType(file.contentType).contentLength(file.bytes.size.toLong())
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(file.filename).build().toString())
            .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
            .body(file.bytes)
    }

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
