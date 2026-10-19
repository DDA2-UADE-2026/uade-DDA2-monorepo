package com.uade.dda2.server.feature.application.controller

import com.uade.dda2.server.error.ErrorResponse
import com.uade.dda2.server.feature.application.dto.request.CreateApplicationResolutionRequest
import com.uade.dda2.server.feature.application.dto.response.ApplicationResolutionResponse
import com.uade.dda2.server.feature.application.service.ApplicationResolutionService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/admin/applications/{id}/resolutions", produces = ["application/json"])
@Tag(name = "Resoluciones de solicitudes", description = "Decision administrativa e historial inmutable de una solicitud.")
@ApiResponse(responseCode = "401", description = "Sin acceso valido o usuario inexistente/inactivo.", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
@ApiResponse(responseCode = "403", description = "El rol activo no conserva el permiso requerido en JWT y base de datos.", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
@ApiResponse(responseCode = "404", description = "Solicitud inexistente.", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
class AdminApplicationResolutionController(private val service: ApplicationResolutionService) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('applications:management:resolve')")
    @Operation(summary = "Resolver una solicitud evaluada", description =
        "Requiere evaluacion profesional y ninguna actuacion SCHEDULED. Desde IN_EVALUATION permite APPROVED, REJECTED o WAITLISTED; " +
        "desde WAITLISTED solo APPROVED o REJECTED. Cada requisito vigente requiere un resultado explicito. Aprobar o esperar exige " +
        "requisitos cumplidos, documentos obligatorios VALID, edicion ACTIVE no vencida y ninguna aprobacion incompatible con fechas " +
        "superpuestas. Aprobar ocupa un cupo; esperar exige cupo agotado. Responsable y fecha los obtiene el servidor. " +
        "Un conflicto no cambia el estado ni el cupo. No se admite corregir una resolucion ni promocion automatica.")
    @ApiResponse(responseCode = "201", description = "Resolucion registrada con responsable, fecha y controles historicos.", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "400", description = "Decision/motivo invalidos, campos extra o checklist incompleto, duplicado o ajeno.", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @ApiResponse(responseCode = "409", description = "Transicion invalida, evaluacion ausente, actuaciones pendientes, requisitos/documentos incumplidos, edicion no habilitada, incompatibilidad o cupo no acorde a la decision.", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    fun create(@PathVariable id: UUID, @RequestBody request: CreateApplicationResolutionRequest): ApplicationResolutionResponse =
        service.create(id, request)

    @GetMapping
    @PreAuthorize("hasAuthority('applications:management:view')")
    @Operation(summary = "Consultar el historial de resoluciones", description =
        "Solo administracion. Devuelve las resoluciones en orden cronologico con motivo, responsable y copia de los requisitos evaluados. " +
        "Una solicitud sin resoluciones devuelve una lista vacia.")
    @ApiResponse(responseCode = "200", description = "Historial cronologico.", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "400", description = "Identificador invalido.", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    fun list(@PathVariable id: UUID): List<ApplicationResolutionResponse> = service.list(id)
}
