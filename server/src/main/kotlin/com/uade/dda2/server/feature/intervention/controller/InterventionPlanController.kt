package com.uade.dda2.server.feature.intervention.controller

import com.uade.dda2.server.error.ErrorResponse
import com.uade.dda2.server.feature.intervention.dto.request.*
import com.uade.dda2.server.feature.intervention.dto.response.*
import com.uade.dda2.server.feature.intervention.service.*
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
@RequestMapping("/api/applications/{id}/intervention-plan", produces = ["application/json"])
@Tag(name = "Planes de intervencion", description = "Plan profesional por caso y seguimientos inmutables de sus acciones.")
@ApiResponse(responseCode = "400", description = "Cuerpo, textos, fechas, UUID o estado solicitado invalidos; campos adicionales no admitidos.", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
@ApiResponse(responseCode = "401", description = "Sin acceso valido o usuario inexistente/inactivo.", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
@ApiResponse(responseCode = "403", description = "Permiso ausente del JWT o rol activo vigente en base de datos.", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
@ApiResponse(responseCode = "404", description = "Solicitud, plan o accion inexistentes o ajenos a la ruta.", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
class InterventionPlanController(private val service: InterventionPlanService) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('$INTERVENTION_CREATE_PERMISSION')")
    @Operation(summary = "Crear el plan de intervencion de un caso", description =
        "Un unico plan por solicitud evaluada en IN_EVALUATION, IN_VISIT, WAITLISTED, APPROVED, REJECTED o CLOSED. " +
        "Solo el trabajador asignado puede crearlo. Exige objetivos y acciones no vacios, descripciones y fechas inclusivas " +
        "con inicio no posterior al fin previsto. Admite planificacion pasada o futura sin depender de la edicion. " +
        "Conserva estructura y orden iniciales, asigna el responsable autenticado y crea acciones PENDING. " +
        "Actualiza solo updatedAt de la solicitud; no cambia estado, resolucion, beneficios, visitas ni cupos.")
    @ApiResponse(responseCode = "201", description = "Plan, objetivos y acciones registrados y auditados.", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "409", description = "Plan duplicado, falta de evaluacion/asignacion, otro trabajador o etapa no admitida.", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    fun create(@PathVariable id: UUID, @RequestBody request: CreateInterventionPlanRequest): InterventionPlanResponse = service.create(id, request)

    @PostMapping("/actions/{actionId}/follow-ups")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('$INTERVENTION_FOLLOW_UP_PERMISSION')")
    @Operation(summary = "Registrar avance o resultado de una accion", description =
        "Solo el trabajador asignado y responsable del plan. PENDING e IN_PROGRESS admiten IN_PROGRESS o COMPLETED; " +
        "COMPLETED es definitivo y exige resultado. Notas obligatorias. La fecha real va desde el inicio hasta hoy " +
        "en la zona configurada, sin retroceder respecto al ultimo seguimiento. Admite carga historica anterior al alta " +
        "del plan, registros del mismo dia y acciones vencidas. Continua despues del rechazo/cierre de la solicitud. " +
        "Confirma historial, estado y auditoria juntos, sin cerrar solicitudes ni alterar cupos.")
    @ApiResponse(responseCode = "201", description = "Seguimiento inmutable registrado; estado de accion actualizado.", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "409", description = "Sin trabajador, actor distinto del responsable o accion ya completada.", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    fun followUp(@PathVariable id: UUID, @PathVariable actionId: UUID,
        @RequestBody request: CreateInterventionFollowUpRequest): InterventionFollowUpResponse = service.followUp(id, actionId, request)
}
