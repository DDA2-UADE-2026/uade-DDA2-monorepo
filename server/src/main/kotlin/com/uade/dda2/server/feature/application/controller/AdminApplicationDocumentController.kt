package com.uade.dda2.server.feature.application.controller

import com.uade.dda2.server.error.ErrorResponse
import com.uade.dda2.server.feature.application.dto.request.ReviewApplicationDocumentRequest
import com.uade.dda2.server.feature.application.dto.response.ApplicationDocumentResponse
import com.uade.dda2.server.feature.application.service.ApplicationDocumentService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@RestController
@RequestMapping("/api/admin/applications/{applicationId}/documents")
@Tag(name = "Revisión de documentos de solicitudes", description = "Entrega asistida, consulta y revisión administrativa de documentos protegidos.")
class AdminApplicationDocumentController(private val service: ApplicationDocumentService) {
    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    @PreAuthorize("hasAuthority('applications:management:documents:view')")
    @Operation(summary = "Listar documentos de cualquier solicitud")
    fun list(@PathVariable applicationId: UUID): List<ApplicationDocumentResponse> = service.listAdmin(applicationId)

    @GetMapping("/{applicationDocumentId}/content")
    @PreAuthorize("hasAuthority('applications:management:documents:view')")
    @Operation(summary = "Ver o descargar un documento de una solicitud", description = "Devuelve el contenido inline con caché privada deshabilitada.")
    @ApiResponse(responseCode = "200", description = "Contenido del archivo.", content = [Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE, schema = Schema(type = "string", format = "binary"))])
    fun content(@PathVariable applicationId: UUID, @PathVariable applicationDocumentId: UUID): ResponseEntity<ByteArray> =
        inline(service.contentAdmin(applicationId, applicationDocumentId))

    @PutMapping("/{requirementId}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    @PreAuthorize("hasAuthority('applications:management:documents:manage')")
    @Operation(
        summary = "Cargar o reemplazar un documento en nombre del titular",
        description = "Entrega asistida para completar un trámite iniciado en la ventanilla. " +
            "Admite PDF, JPEG o PNG de hasta 10 MB. Reemplazar conserva la entrega y reinicia su revisión a PENDING. " +
            "El archivo queda atribuido al administrativo que lo sube.",
    )
    @ApiResponse(responseCode = "201", description = "Primera entrega para el requisito.")
    @ApiResponse(responseCode = "200", description = "Entrega existente reemplazada.")
    @ApiResponse(responseCode = "400", description = "Archivo vacío, nombre, extensión, MIME o firma inválidos.", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @ApiResponse(responseCode = "413", description = "Archivo mayor a 10 MB.", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @ApiResponse(responseCode = "409", description = "La solicitud se encuentra resuelta.", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    fun put(
        @PathVariable applicationId: UUID,
        @PathVariable requirementId: UUID,
        @RequestPart("file") file: MultipartFile,
    ): ResponseEntity<ApplicationDocumentResponse> {
        val result = service.putAdmin(applicationId, requirementId, file)
        return ResponseEntity.status(if (result.created) 201 else 200).body(result.document)
    }

    @PatchMapping("/{applicationDocumentId}/review", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    @PreAuthorize("hasAuthority('applications:management:documents:review')")
    @Operation(summary = "Revisar un documento pendiente", description = "Solo admite VALID u OBSERVED desde PENDING. OBSERVED exige observación y VALID no la admite.")
    @ApiResponse(responseCode = "409", description = "Solicitud resuelta, entrega ya revisada o revisión inconsistente.", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    fun review(
        @PathVariable applicationId: UUID,
        @PathVariable applicationDocumentId: UUID,
        @Valid @RequestBody request: ReviewApplicationDocumentRequest,
    ): ApplicationDocumentResponse = service.reviewAdmin(applicationId, applicationDocumentId, request)
}
