package com.uade.dda2.server.feature.application.controller

import com.uade.dda2.server.feature.application.dto.request.CreateApplicationRequest
import com.uade.dda2.server.feature.application.dto.response.ApplicationListResponse
import com.uade.dda2.server.feature.application.dto.response.ApplicationResponse
import com.uade.dda2.server.feature.application.service.ApplicationService
import com.uade.dda2.server.error.ErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.headers.Header
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.util.UUID

@RestController
@RequestMapping("/api/applications", produces = ["application/json"])
@Tag(name = "Solicitudes propias", description = "Presentación y consulta de solicitudes del usuario autenticado.")
class ApplicationController(private val service: ApplicationService) {
    @PostMapping
    @PreAuthorize("hasAuthority('applications:own:create')")
    @Operation(summary = "Presentar una solicitud propia", description =
        "Requiere applications:own:create en el rol activo. La convocatoria debe estar OPEN y vigente y la edición ACTIVE. " +
        "No consume ni requiere cupo disponible. Una sola solicitud por usuario y convocatoria; en otra convocatoria de la misma " +
        "edición solo se permite si todas las anteriores están REJECTED o CLOSED. El cuerpo solo admite enrollmentPeriodId.")
    @ApiResponse(responseCode = "201", description = "Solicitud presentada con número único.", headers = [
        Header(name = "Location", description = "URL del detalle propio.", schema = Schema(type = "string")),
        Header(name = "Idempotency-Replayed", schema = Schema(type = "boolean", example = "false")),
    ])
    @ApiResponse(responseCode = "200", description = "Reintento: se devuelve la solicitud original sin crear otra ni repetir la auditoría.", headers = [
        Header(name = "Location", description = "URL del detalle propio.", schema = Schema(type = "string")),
        Header(name = "Idempotency-Replayed", schema = Schema(type = "boolean", example = "true")),
    ])
    @ApiResponse(responseCode = "404", description = "Convocatoria inexistente.", content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))])
    @ApiResponse(responseCode = "409", description = "Convocatoria no habilitada, solicitud duplicada o clave reutilizada con otro payload.", content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))])
    fun submit(
        @Valid @RequestBody request: CreateApplicationRequest,
        @Parameter(description = "Opcional. 1–128 caracteres ASCII visibles sin espacios, sensible a mayúsculas. " +
            "Se conserva por usuario durante la vida de la solicitud. Misma clave y convocatoria: replay; otra convocatoria: 409.")
        @RequestHeader(name = "Idempotency-Key", required = false) idempotencyKey: String?,
    ): ResponseEntity<ApplicationResponse> {
        val result = service.submit(request, idempotencyKey)
        return ResponseEntity.status(if (result.replayed) 200 else 201)
            .location(URI.create("/api/applications/${result.application.id}"))
            .header("Idempotency-Replayed", result.replayed.toString())
            .body(result.application)
    }

    @GetMapping
    @PreAuthorize("hasAuthority('applications:own:view')")
    @Operation(summary = "Listar mis solicitudes", description = "Solo solicitudes propias, ordenadas por número descendente. Requiere applications:own:view.")
    fun list(
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
    ): ApplicationListResponse = service.list(page, size)

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('applications:own:view')")
    @Operation(summary = "Consultar una solicitud propia", description = "Requiere applications:own:view. Una solicitud ajena se responde como inexistente.")
    @ApiResponse(responseCode = "404", description = "Solicitud inexistente o perteneciente a otro usuario.", content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))])
    fun get(@PathVariable id: UUID): ApplicationResponse = service.get(id)
}
