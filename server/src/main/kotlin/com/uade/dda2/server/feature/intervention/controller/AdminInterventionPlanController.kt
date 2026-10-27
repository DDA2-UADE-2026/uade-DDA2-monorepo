package com.uade.dda2.server.feature.intervention.controller

import com.uade.dda2.server.error.ErrorResponse
import com.uade.dda2.server.feature.intervention.dto.response.*
import com.uade.dda2.server.feature.intervention.service.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/admin/applications/{id}/intervention-plan", produces = ["application/json"])
@Tag(name = "Planes de intervencion", description = "Plan profesional por caso y seguimientos inmutables de sus acciones.")
@PreAuthorize("hasAuthority('$INTERVENTION_VIEW_PERMISSION')")
@ApiResponse(responseCode = "400", description = "Identificador invalido.", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
@ApiResponse(responseCode = "401", description = "Sin acceso valido o usuario inexistente/inactivo.", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
@ApiResponse(responseCode = "403", description = "Sin permiso vigente en JWT y rol activo de base de datos.", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
@ApiResponse(responseCode = "404", description = "Solicitud, plan o accion inexistentes o ajenos a la ruta.", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
class AdminInterventionPlanController(private val service: InterventionPlanService) {
    @GetMapping
    @Operation(summary = "Consultar el plan y estados actuales de sus acciones", description =
        "Lectura profesional completa por permiso; no exige ser el trabajador asignado. Conserva orden de objetivos y acciones. " +
        "Incluye ciudadano del caso, responsable y fecha UTC. La consulta ciudadana usa DTO propios y rutas separadas.")
    @ApiResponse(responseCode = "200", description = "Plan con estructura inicial y estados actuales.", useReturnTypeSchema = true)
    fun get(@PathVariable id: UUID): InterventionPlanResponse = service.get(id)

    @GetMapping("/actions/{actionId}/follow-ups")
    @Operation(summary = "Consultar seguimientos y resultados de una accion", description =
        "Historial inmutable en secuencia ascendente. Incluye estados anterior/resultante, fecha real, notas, resultado, autor " +
        "y registro UTC. citizenPublication es null si nunca se publico; en otro caso conserva la version actual, incluso retirada, " +
        "con resumen, visibilidad, ultima publicacion y autor/fecha de gestion. Una accion existente sin seguimientos devuelve []. Requiere permiso profesional de lectura.")
    @ApiResponse(responseCode = "200", description = "Historial completo de la accion.", useReturnTypeSchema = true)
    fun history(@PathVariable id: UUID, @PathVariable actionId: UUID): List<InterventionFollowUpResponse> = service.history(id, actionId)
}
