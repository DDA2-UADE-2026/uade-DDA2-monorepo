package com.uade.dda2.server.feature.application.controller

import com.uade.dda2.server.error.ErrorResponse
import com.uade.dda2.server.feature.application.dto.request.CancelApplicationVisitRequest
import com.uade.dda2.server.feature.application.dto.request.CreateApplicationVisitRequest
import com.uade.dda2.server.feature.application.dto.request.RescheduleApplicationVisitRequest
import com.uade.dda2.server.feature.application.dto.response.ApplicationVisitResponse
import com.uade.dda2.server.feature.application.service.APPLICATION_VISIT_CREATE_PERMISSION
import com.uade.dda2.server.feature.application.service.APPLICATION_VISIT_MANAGE_PERMISSION
import com.uade.dda2.server.feature.application.service.ApplicationVisitService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.PutMapping
import java.util.UUID

@RestController
@RequestMapping("/api/applications/{applicationId}/visits", produces = ["application/json"])
@Tag(name = "Visitas sociales", description = "Programacion, reprogramacion y cancelacion de actuaciones sociales.")
class ApplicationVisitController(private val service: ApplicationVisitService) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('$APPLICATION_VISIT_CREATE_PERMISSION')")
    @Operation(
        operationId = "createApplicationVisit",
        summary = "Programar una visita o entrevista social",
        description = "Requiere applications:visits:create en el rol activo. La solicitud debe tener una evaluacion " +
            "y estar asignada al usuario autenticado. El servidor calcula el fin y registra la actuacion como SCHEDULED.",
    )
    @ApiResponse(responseCode = "201", description = "Visita o entrevista programada.", useReturnTypeSchema = true)
    @ApiResponse(
        responseCode = "400",
        description = "Tipo, fecha, duracion, lugar o cuerpo invalidos.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    @ApiResponse(
        responseCode = "404",
        description = "Solicitud inexistente.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    @ApiResponse(
        responseCode = "409",
        description = "Falta la evaluacion, el estado o responsable no son validos, o existe un conflicto de agenda.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    fun create(
        @PathVariable applicationId: UUID,
        @RequestBody request: CreateApplicationVisitRequest,
    ): ApplicationVisitResponse = service.create(applicationId, request)

    @PutMapping("/{visitId}/schedule")
    @PreAuthorize("hasAuthority('$APPLICATION_VISIT_MANAGE_PERMISSION')")
    @Operation(
        operationId = "rescheduleApplicationVisit",
        summary = "Reprogramar una visita o entrevista social",
        description = "Requiere applications:visits:manage en el rol activo. Solo el responsable que continua " +
            "asignado a la solicitud puede cambiar el horario, la duracion y el lugar de una actuacion futura SCHEDULED.",
    )
    @ApiResponse(responseCode = "200", description = "Actuacion reprogramada.", useReturnTypeSchema = true)
    @ApiResponse(
        responseCode = "400",
        description = "Fecha, duracion, lugar o cuerpo invalidos, o la solicitud no introduce cambios.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    @ApiResponse(
        responseCode = "404",
        description = "Solicitud o actuacion inexistente.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    @ApiResponse(
        responseCode = "409",
        description = "La actuacion no es modificable, fue modificada concurrentemente o existe un conflicto de agenda.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    fun reschedule(
        @PathVariable applicationId: UUID,
        @PathVariable visitId: UUID,
        @RequestBody request: RescheduleApplicationVisitRequest,
    ): ApplicationVisitResponse = service.reschedule(applicationId, visitId, request)

    @PatchMapping("/{visitId}/cancel")
    @PreAuthorize("hasAuthority('$APPLICATION_VISIT_MANAGE_PERMISSION')")
    @Operation(
        operationId = "cancelApplicationVisit",
        summary = "Cancelar una visita o entrevista social",
        description = "Requiere applications:visits:manage en el rol activo. La cancelacion exige un motivo, " +
            "libera la agenda y puede devolver la solicitud a IN_EVALUATION.",
    )
    @ApiResponse(responseCode = "200", description = "Actuacion cancelada.", useReturnTypeSchema = true)
    @ApiResponse(
        responseCode = "400",
        description = "Motivo o cuerpo invalidos.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    @ApiResponse(
        responseCode = "404",
        description = "Solicitud o actuacion inexistente.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    @ApiResponse(
        responseCode = "409",
        description = "La actuacion no es modificable, pertenece a otro trabajador o fue modificada concurrentemente.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    fun cancel(
        @PathVariable applicationId: UUID,
        @PathVariable visitId: UUID,
        @RequestBody request: CancelApplicationVisitRequest,
    ): ApplicationVisitResponse = service.cancel(applicationId, visitId, request)
}
