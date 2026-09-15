package com.uade.dda2.server.feature.application.controller

import com.uade.dda2.server.error.ErrorResponse
import com.uade.dda2.server.feature.application.dto.request.CreateAssistedApplicationRequest
import com.uade.dda2.server.feature.application.dto.response.AdminApplicationListResponse
import com.uade.dda2.server.feature.application.dto.response.AdminApplicationResponse
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
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/admin/applications", produces = ["application/json"])
@Validated
@Tag(name = "Solicitudes asistidas", description = "Registro y consulta administrativa de solicitudes de cualquier titular.")
class AdminApplicationController(private val service: ApplicationService) {
    @PostMapping
    @PreAuthorize("hasAuthority('applications:management:create')")
    @Operation(summary = "Registrar una solicitud asistida", description =
        "Requiere applications:management:create en el rol activo. userId identifica al titular en users, sin restricción " +
        "de jurisdicción ni integración externa. registeredByUserId se obtiene del JWT y no se acepta en el cuerpo. " +
        "Conserva las mismas reglas de convocatoria, edición, duplicados e idempotencia que la presentación propia. " +
        "Para solicitar para uno mismo se debe usar POST /api/applications con su permiso propio. " +
        "Registrar para otra persona no habilita por sí mismo a consultar sus solicitudes: la consulta administrativa " +
        "exige applications:management:view.")
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

    @GetMapping
    @PreAuthorize("hasAuthority('applications:management:view')")
    @Operation(summary = "Listar solicitudes de cualquier titular", description =
        "Requiere applications:management:view en el rol activo. Devuelve una página con las solicitudes de todos los " +
        "titulares, ordenadas por número descendente, sin filtrar por el administrativo que las registró. " +
        "Cada elemento identifica al titular con su nombre y correo de users; el detalle completo está en " +
        "GET /api/admin/applications/{id}.")
    @ApiResponse(responseCode = "200", description = "Página de solicitudes.", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "400", description = "Paginación fuera de rango.", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @ApiResponse(responseCode = "401", description = "Sin JWT de acceso válido o administrativo inexistente/inactivo.", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @ApiResponse(responseCode = "403", description = "Sin permiso en el rol activo o rol retirado.", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    fun list(
        @Parameter(description = "Número de página, comenzando en cero.", example = "0")
        @Min(value = 0, message = "La página no puede ser negativa.")
        @RequestParam(defaultValue = "0") page: Int,

        @Parameter(description = "Cantidad de elementos por página, entre 1 y 100.", example = "20")
        @Min(value = 1, message = "El tamaño de página debe ser mayor a cero.")
        @Max(value = 100, message = "El tamaño de página no puede superar 100 elementos.")
        @RequestParam(defaultValue = "20") size: Int,
    ): AdminApplicationListResponse = service.listAdmin(page, size)

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('applications:management:view')")
    @Operation(summary = "Consultar una solicitud de cualquier titular", description =
        "Requiere applications:management:view en el rol activo. Devuelve la entidad completa, incluidos el ticket de " +
        "origen, el motivo de resolución, el trabajador asignado y la clave de idempotencia, que la vista propia no expone. " +
        "No devuelve los archivos: sus metadatos y contenido siguen en las rutas de documentos con sus propios permisos.")
    @ApiResponse(responseCode = "200", description = "Detalle completo de la solicitud.", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "401", description = "Sin JWT de acceso válido o administrativo inexistente/inactivo.", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @ApiResponse(responseCode = "403", description = "Sin permiso en el rol activo o rol retirado.", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @ApiResponse(responseCode = "404", description = "Solicitud inexistente.", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    fun get(@PathVariable id: UUID): AdminApplicationResponse = service.getAdmin(id)
}
