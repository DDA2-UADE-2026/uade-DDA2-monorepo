package com.uade.dda2.server.feature.application.controller

import com.uade.dda2.server.error.ErrorResponse
import com.uade.dda2.server.feature.application.dto.request.CreateAssistedApplicationRequest
import com.uade.dda2.server.feature.application.dto.response.ApplicationResponse
import com.uade.dda2.server.feature.application.service.ApplicationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.headers.Header
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/applications", produces = ["application/json"])
@Tag(name = "Solicitudes asistidas", description = "Registro administrativo de solicitudes para ciudadanos existentes.")
class AdminApplicationController(private val service: ApplicationService) {
    @PostMapping
    @PreAuthorize("hasAuthority('applications:management:create')")
    @Operation(summary = "Registrar una solicitud asistida", description =
        "Requiere applications:management:create en el rol activo. userId identifica al titular en users, sin restricción " +
        "de jurisdicción ni integración externa. registeredByUserId se obtiene del JWT y no se acepta en el cuerpo. " +
        "Conserva las mismas reglas de convocatoria, edición, duplicados e idempotencia que la presentación propia. " +
        "Para solicitar para uno mismo se debe usar POST /api/applications con su permiso propio. " +
        "Registrar para otra persona no habilita a consultar sus solicitudes: no se agrega un detalle administrativo.")
    @ApiResponse(responseCode = "201", description = "Solicitud registrada para el titular indicado.", headers = [
        Header(name = "Idempotency-Replayed", schema = Schema(type = "boolean", example = "false")),
    ])
    @ApiResponse(responseCode = "200", description = "Reintento: devuelve la solicitud existente y conserva al registrante original.", headers = [
        Header(name = "Idempotency-Replayed", schema = Schema(type = "boolean", example = "true")),
    ])
    @ApiResponse(responseCode = "400", description = "Cuerpo o clave de idempotencia inválidos.", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @ApiResponse(responseCode = "401", description = "Sin JWT de acceso válido o administrativo inexistente/inactivo.", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @ApiResponse(responseCode = "403", description = "Sin permiso en el rol activo, rol retirado o intento de presentación asistida para uno mismo.", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @ApiResponse(responseCode = "404", description = "Titular o convocatoria inexistentes.", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @ApiResponse(responseCode = "409", description = "Convocatoria o edición no habilitadas, solicitud duplicada o clave reutilizada con otro payload.", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    fun submit(
        @Valid @RequestBody request: CreateAssistedApplicationRequest,
        @Parameter(description = "Opcional. 1–128 caracteres ASCII visibles sin espacios. Se asocia al titular userId, " +
            "compartida con las presentaciones propias y asistidas. Misma clave y convocatoria devuelve la original; " +
            "otra convocatoria para el mismo titular devuelve 409. Cambiar el administrativo no cambia al registrante original.")
        @RequestHeader(name = "Idempotency-Key", required = false) idempotencyKey: String?,
    ): ResponseEntity<ApplicationResponse> {
        val result = service.submitAssisted(request, idempotencyKey)
        return ResponseEntity.status(if (result.replayed) 200 else 201)
            .header("Idempotency-Replayed", result.replayed.toString())
            .body(result.application)
    }
}
