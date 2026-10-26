package com.uade.dda2.server.feature.intervention.controller

import com.uade.dda2.server.error.ErrorResponse
import com.uade.dda2.server.feature.intervention.dto.request.PublishInterventionFollowUpRequest
import com.uade.dda2.server.feature.intervention.dto.response.InterventionPublicationResponse
import com.uade.dda2.server.feature.intervention.service.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/applications/{id}/intervention-plan/actions/{actionId}/follow-ups/{followUpId}/citizen-publication", produces = ["application/json"])
@PreAuthorize("hasAuthority('$INTERVENTION_PUBLISH_PERMISSION')")
@Tag(name = "Planes de intervencion", description = "Gestion profesional de resumenes visibles al ciudadano, sin modificar seguimientos originales.")
@ApiResponse(responseCode = "400", description = "Resumen, cuerpo o identificadores invalidos; campos adicionales rechazados.", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
@ApiResponse(responseCode = "401", description = "Sin acceso valido o usuario inexistente/inactivo.", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
@ApiResponse(responseCode = "403", description = "Permiso ausente del JWT o rol activo vigente.", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
@ApiResponse(responseCode = "404", description = "Solicitud, plan, accion o seguimiento inexistentes o no asociados a la ruta.", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
@ApiResponse(responseCode = "409", description = "Actor distinto del trabajador asignado/responsable o sin asignacion.", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
class InterventionPublicationController(private val service: InterventionPublicationService) {
    @PutMapping
    @Operation(summary = "Publicar, corregir o volver a habilitar un resumen ciudadano", description =
        "Solo el trabajador asignado y responsable. Summary obligatorio, recortado, de 1 a 2000 caracteres. " +
        "Gestion separada del seguimiento inmutable; nunca copia notes/result. Admite acciones completadas y casos rechazados/cerrados. " +
        "Mismo resumen ya visible es idempotente y no repite fechas ni auditoria. Obtiene actor y fechas del servidor.")
    @ApiResponse(responseCode = "201", description = "Publicacion creada. Requiere applications:interventions:follow-ups:publish en JWT y rol activo.", content = [Content(
        schema = Schema(implementation = InterventionPublicationResponse::class), examples = [ExampleObject(value = PUBLICATION_EXAMPLE)])])
    @ApiResponse(responseCode = "200", description = "Publicacion actualizada, rehabilitada o repetida sin cambios.", useReturnTypeSchema = true)
    fun publish(@PathVariable id: UUID, @PathVariable actionId: UUID, @PathVariable followUpId: UUID,
        @RequestBody request: PublishInterventionFollowUpRequest): ResponseEntity<InterventionPublicationResponse> {
        val result = service.publish(id, actionId, followUpId, request)
        return ResponseEntity.status(if (result.created) HttpStatus.CREATED else HttpStatus.OK).body(result.publication)
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Retirar la visibilidad ciudadana de un seguimiento", description =
        "Conserva resumen y registro profesional. Solo trabajador asignado/responsable. " +
        "Repetir el retiro o retirar un seguimiento valido nunca publicado devuelve 204 sin cambios. " +
        "Nuevas consultas ciudadanas no incluyen publicaciones retiradas ni las cuentan.")
    @ApiResponse(responseCode = "204", description = "Retirada o ya no visible.")
    fun withdraw(@PathVariable id: UUID, @PathVariable actionId: UUID, @PathVariable followUpId: UUID) = service.withdraw(id, actionId, followUpId)
}
