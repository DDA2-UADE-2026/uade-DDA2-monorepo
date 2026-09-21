package com.uade.dda2.server.feature.program.controller

import com.uade.dda2.server.error.ErrorResponse
import com.uade.dda2.server.feature.program.dto.admin.response.ProgramImageResponse
import com.uade.dda2.server.feature.program.service.ProgramImageService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@RestController
@RequestMapping("/api/admin/programs/{programId}/image")
@Tag(name = "Imágenes de programas", description = "Administración de las imágenes de portada de los programas.")
class AdminProgramImageController(
    private val programImageService: ProgramImageService,
) {
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('programs:management:create')")
    @Operation(operationId = "createProgramImage", summary = "Agregar la imagen de un programa", description = "Admite una imagen JPEG o PNG de hasta 10 MB.")
    @ApiResponse(responseCode = "201", description = "Imagen creada.", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "400", description = "Archivo vacío, nombre, extensión, MIME o firma inválidos.", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @ApiResponse(responseCode = "404", description = "Programa inexistente.", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @ApiResponse(responseCode = "409", description = "El programa ya posee una imagen.", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @ApiResponse(responseCode = "413", description = "Imagen mayor a 10 MB.", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    fun create(
        @Parameter(description = "UUID del programa.")
        @PathVariable programId: UUID,
        @RequestPart("file") file: MultipartFile,
    ): ProgramImageResponse = programImageService.create(programId, file)

    @PutMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
    @PreAuthorize("hasAuthority('programs:management:edit')")
    @Operation(operationId = "updateProgramImage", summary = "Reemplazar la imagen de un programa", description = "Reemplaza la portada existente por una imagen JPEG o PNG de hasta 10 MB.")
    @ApiResponse(responseCode = "200", description = "Imagen reemplazada.", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "400", description = "Archivo vacío, nombre, extensión, MIME o firma inválidos.", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @ApiResponse(responseCode = "404", description = "Programa o imagen inexistente.", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @ApiResponse(responseCode = "413", description = "Imagen mayor a 10 MB.", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    fun update(
        @Parameter(description = "UUID del programa.")
        @PathVariable programId: UUID,
        @RequestPart("file") file: MultipartFile,
    ): ProgramImageResponse = programImageService.update(programId, file)

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('programs:management:edit')")
    @Operation(operationId = "deleteProgramImage", summary = "Eliminar la imagen de un programa")
    @ApiResponse(responseCode = "204", description = "Imagen eliminada.")
    @ApiResponse(responseCode = "404", description = "Programa o imagen inexistente.", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    fun delete(
        @Parameter(description = "UUID del programa.")
        @PathVariable programId: UUID,
    ) = programImageService.delete(programId)
}
