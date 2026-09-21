package com.uade.dda2.server.feature.program.controller

import com.uade.dda2.server.error.ErrorResponse
import com.uade.dda2.server.feature.program.dto.ProgramImageContent
import com.uade.dda2.server.feature.program.service.ProgramImageService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.nio.charset.StandardCharsets
import java.util.UUID

@RestController
@RequestMapping("/api/images")
@Tag(name = "Imágenes públicas", description = "Consulta pública del contenido de imágenes.")
class ImageController(
    private val programImageService: ProgramImageService,
) {
    @GetMapping("/{imageId}")
    @Operation(operationId = "getPublicImage", summary = "Obtener una imagen", description = "Devuelve públicamente los bytes de una imagen de programa.")
    @ApiResponse(responseCode = "200", description = "Contenido de la imagen.", content = [Content(mediaType = "image/jpeg", schema = Schema(type = "string", format = "binary")), Content(mediaType = "image/png", schema = Schema(type = "string", format = "binary"))])
    @ApiResponse(responseCode = "404", description = "Imagen inexistente.", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    fun content(
        @PathVariable imageId: UUID,
    ): ResponseEntity<ByteArray> = inline(programImageService.content(imageId))
}

private fun inline(content: ProgramImageContent): ResponseEntity<ByteArray> =
    ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(content.contentType))
        .contentLength(content.sizeBytes)
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.inline()
                .filename(content.originalName, StandardCharsets.UTF_8)
                .build()
                .toString(),
        )
        .header(HttpHeaders.CACHE_CONTROL, "no-cache")
        .body(content.content)
