package com.uade.dda2.server.feature.application.controller

import com.uade.dda2.server.error.ErrorResponse
import com.uade.dda2.server.feature.application.dto.response.ApplicationDocumentContent
import com.uade.dda2.server.feature.application.dto.response.ApplicationDocumentResponse
import com.uade.dda2.server.feature.application.service.ApplicationDocumentService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.nio.charset.StandardCharsets
import java.util.UUID

@RestController
@RequestMapping("/api/applications/{applicationId}/documents")
@Tag(name = "Documentos de solicitudes propias", description = "Entrega y consulta protegida de documentos del usuario autenticado.")
class ApplicationDocumentController(private val service: ApplicationDocumentService) {
    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    @PreAuthorize("hasAuthority('applications:own:documents:view')")
    @Operation(summary = "Listar los documentos entregados en una solicitud propia")
    @ApiResponse(responseCode = "404", description = "Solicitud inexistente o ajena.", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    fun list(@PathVariable applicationId: UUID): List<ApplicationDocumentResponse> = service.listOwn(applicationId)

    @PutMapping("/{requirementId}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    @PreAuthorize("hasAuthority('applications:own:documents:manage')")
    @Operation(summary = "Cargar o reemplazar un documento", description = "Admite PDF, JPEG o PNG de hasta 10 MB. Reemplazar conserva la entrega y reinicia su revisión a PENDING.")
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
        val result = service.putOwn(applicationId, requirementId, file)
        return ResponseEntity.status(if (result.created) 201 else 200).body(result.document)
    }

    @DeleteMapping("/{applicationDocumentId}")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('applications:own:documents:manage')")
    @Operation(summary = "Eliminar un documento entregado")
    fun delete(@PathVariable applicationId: UUID, @PathVariable applicationDocumentId: UUID) =
        service.deleteOwn(applicationId, applicationDocumentId)

    @GetMapping("/{applicationDocumentId}/content")
    @PreAuthorize("hasAuthority('applications:own:documents:view')")
    @Operation(summary = "Ver o descargar de forma protegida un documento propio", description = "Devuelve el contenido inline con caché privada deshabilitada.")
    @ApiResponse(responseCode = "200", description = "Contenido del archivo.", content = [Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE, schema = Schema(type = "string", format = "binary"))])
    fun content(@PathVariable applicationId: UUID, @PathVariable applicationDocumentId: UUID): ResponseEntity<ByteArray> =
        inline(service.contentOwn(applicationId, applicationDocumentId))
}

internal fun inline(content: ApplicationDocumentContent): ResponseEntity<ByteArray> = ResponseEntity.ok()
    .contentType(MediaType.parseMediaType(content.contentType))
    .contentLength(content.sizeBytes)
    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(content.originalName, StandardCharsets.UTF_8).build().toString())
    .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
    .body(content.content)
