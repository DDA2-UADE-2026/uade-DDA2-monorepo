package com.uade.dda2.server.feature.intervention.controller

import com.uade.dda2.server.error.ErrorResponse
import com.uade.dda2.server.feature.intervention.dto.response.*
import com.uade.dda2.server.feature.intervention.service.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@Validated
@RequestMapping("/api/intervention-plans", produces = ["application/json"])
@PreAuthorize("hasAuthority('$INTERVENTION_OWN_VIEW_PERMISSION')")
@Tag(name = "Planes de intervencion propios", description = "Objetivos, acciones y unicamente seguimientos con resumen publico vigente del titular autenticado. Requiere applications:own:interventions:view en JWT y rol activo; renovar tokens anteriores.")
@ApiResponse(responseCode = "401", description = "Sin autenticacion valida o usuario inexistente/inactivo.", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
@ApiResponse(responseCode = "403", description = "Permiso ausente en JWT o rol activo vigente.", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
@ApiResponse(responseCode = "400", description = "Identificadores o paginacion invalidos.", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
class CitizenInterventionPlanController(private val service: CitizenInterventionPlanService) {
    @GetMapping
    @Operation(operationId = "listOwnInterventionPlans", summary = "Consultar mis planes de intervencion", description =
        "Titular obtenido de la autenticacion. Incluye todos sus casos, tambien rechazados/cerrados y registros anteriores. " +
        "Orden createdAt/id descendente. Estado calculado: PENDING si todas las acciones estan pendientes, COMPLETED si todas " +
        "terminaron, IN_PROGRESS en cualquier otro caso. Mensaje informativo solo cuando totalElements=0.")
    @ApiResponse(responseCode = "200", description = "Pagina propia, con mensaje si no hay planes.", content = [Content(
        schema = Schema(implementation = CitizenInterventionPlanListResponse::class), examples = [
            ExampleObject(name = "Planes propios", value = OWN_PLANS_EXAMPLE),
            ExampleObject(name = "Sin planes", value = NO_OWN_PLANS_EXAMPLE),
        ])])
    fun list(@RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int): CitizenInterventionPlanListResponse = service.list(page, size)

    @GetMapping("/{planId}")
    @Operation(operationId = "getOwnInterventionPlan", summary = "Consultar objetivos y acciones de mi plan", description =
        "Conserva orden, descripciones y fechas acordadas. Solo estructura publica, sin responsables ni textos profesionales internos. " +
        "No modifica estados, fechas ni reservas. La informacion ajena se trata como inexistente.")
    @ApiResponse(responseCode = "200", description = "Detalle del plan propio y estado calculado.", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "404", description = "Plan inexistente o ajeno.", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    fun get(@PathVariable planId: UUID): CitizenInterventionPlanResponse = service.get(planId)

    @GetMapping("/{planId}/actions/{actionId}/follow-ups")
    @Operation(operationId = "listOwnPublishedInterventionFollowUps", summary = "Consultar seguimientos habilitados de una accion propia", description =
        "Solo publicaciones visibles. Orden por secuencia original ascendente sin publicar esa secuencia ni contar entradas privadas. " +
        "Expone resumen expresamente redactado, fecha real, estado del seguimiento y fecha UTC de publicacion; nunca notes/result originales. " +
        "Mensaje informativo solo cuando el total visible es cero; una pagina fuera de rango conserva totales y message=null si hay publicaciones.")
    @ApiResponse(responseCode = "200", description = "Pagina de seguimientos publicos del titular.", content = [Content(
        schema = Schema(implementation = CitizenInterventionFollowUpListResponse::class), examples = [
            ExampleObject(name = "Resumen expresamente publicado", value = PUBLIC_FOLLOW_UPS_EXAMPLE),
            ExampleObject(name = "Sin publicaciones vigentes", value = NO_PUBLIC_FOLLOW_UPS_EXAMPLE),
        ])])
    @ApiResponse(responseCode = "404", description = "Plan/accion inexistentes, ajenos o no asociados entre si.", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    fun followUps(@PathVariable planId: UUID, @PathVariable actionId: UUID,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int): CitizenInterventionFollowUpListResponse = service.followUps(planId, actionId, page, size)
}
