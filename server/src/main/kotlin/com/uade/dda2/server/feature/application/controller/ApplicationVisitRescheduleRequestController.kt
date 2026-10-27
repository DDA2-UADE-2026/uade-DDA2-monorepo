package com.uade.dda2.server.feature.application.controller

import com.uade.dda2.server.error.ErrorResponse
import com.uade.dda2.server.feature.application.dto.request.CreateApplicationVisitRescheduleRequest
import com.uade.dda2.server.feature.application.dto.request.ReviewApplicationVisitRescheduleRequest
import com.uade.dda2.server.feature.application.dto.response.AdminApplicationVisitRescheduleRequestDetailResponse
import com.uade.dda2.server.feature.application.dto.response.CitizenApplicationVisitRescheduleRequestDetailResponse
import com.uade.dda2.server.feature.application.dto.response.CitizenApplicationVisitRescheduleRequestListResponse
import com.uade.dda2.server.feature.application.service.APPLICATION_OWN_VISIT_RESCHEDULE_REQUEST_CREATE_PERMISSION
import com.uade.dda2.server.feature.application.service.APPLICATION_OWN_VISIT_VIEW_PERMISSION
import com.uade.dda2.server.feature.application.service.APPLICATION_VISIT_RESCHEDULE_REQUEST_REVIEW_PERMISSION
import com.uade.dda2.server.feature.application.service.ApplicationVisitRescheduleRequestService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping(
    "/api/applications/{applicationId}/visits/{visitId}/reschedule-requests",
    produces = ["application/json"],
)
@Validated
@Tag(
    name = "Solicitudes de reprogramacion",
    description = "Solicitud ciudadana, consulta del historial y decision del trabajador responsable.",
)
class ApplicationVisitRescheduleRequestController(
    private val service: ApplicationVisitRescheduleRequestService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('$APPLICATION_OWN_VISIT_RESCHEDULE_REQUEST_CREATE_PERMISSION')")
    @Operation(
        operationId = "createOwnApplicationVisitRescheduleRequest",
        summary = "Solicitar la reprogramacion de una visita propia",
        description = "La identidad se obtiene del JWT. La visita debe estar programada y ser futura. " +
            "La solicitud queda PENDING y no modifica su horario ni lugar.",
    )
    @ApiResponse(responseCode = "201", description = "Solicitud registrada.", useReturnTypeSchema = true)
    @ApiResponse(
        responseCode = "400",
        description = "Motivo o cuerpo invalidos.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    @ApiResponse(
        responseCode = "404",
        description = "Solicitud o visita inexistente, ajena o asociada a otra solicitud.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    @ApiResponse(
        responseCode = "409",
        description = "La visita no admite la solicitud o ya existe otra pendiente.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    fun create(
        @PathVariable applicationId: UUID,
        @PathVariable visitId: UUID,
        @RequestBody request: CreateApplicationVisitRescheduleRequest,
    ): CitizenApplicationVisitRescheduleRequestDetailResponse = service.createOwn(applicationId, visitId, request)

    @GetMapping
    @PreAuthorize("hasAuthority('$APPLICATION_OWN_VISIT_VIEW_PERMISSION')")
    @Operation(
        operationId = "listOwnApplicationVisitRescheduleRequests",
        summary = "Consultar el historial propio de solicitudes de reprogramacion",
        description = "Ordena por fecha e identificador descendentes y omite motivo y comentario.",
    )
    @ApiResponse(responseCode = "200", description = "Pagina del historial propio.", useReturnTypeSchema = true)
    @ApiResponse(
        responseCode = "400",
        description = "Paginacion invalida.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    @ApiResponse(
        responseCode = "404",
        description = "Solicitud o visita inexistente, ajena o asociada a otra solicitud.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    fun list(
        @PathVariable applicationId: UUID,
        @PathVariable visitId: UUID,
        @Parameter(description = "Numero de pagina, comenzando en cero.")
        @Min(0) @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "Elementos por pagina, entre 1 y 100.")
        @Min(1) @Max(100) @RequestParam(defaultValue = "20") size: Int,
    ): CitizenApplicationVisitRescheduleRequestListResponse = service.listOwn(applicationId, visitId, page, size)

    @GetMapping("/{requestId}")
    @PreAuthorize("hasAuthority('$APPLICATION_OWN_VISIT_VIEW_PERMISSION')")
    @Operation(
        operationId = "getOwnApplicationVisitRescheduleRequest",
        summary = "Consultar una solicitud propia de reprogramacion",
        description = "Incluye motivo y comentario de revision, sin identidades internas de trabajadores.",
    )
    @ApiResponse(responseCode = "200", description = "Detalle de la solicitud propia.", useReturnTypeSchema = true)
    @ApiResponse(
        responseCode = "404",
        description = "Solicitud, visita o peticion inexistente o ajena.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    fun get(
        @PathVariable applicationId: UUID,
        @PathVariable visitId: UUID,
        @PathVariable requestId: UUID,
    ): CitizenApplicationVisitRescheduleRequestDetailResponse = service.getOwn(applicationId, visitId, requestId)

    @PatchMapping("/{requestId}/review")
    @PreAuthorize("hasAuthority('$APPLICATION_VISIT_RESCHEDULE_REQUEST_REVIEW_PERMISSION')")
    @Operation(
        operationId = "reviewApplicationVisitRescheduleRequest",
        summary = "Aprobar o rechazar una solicitud de reprogramacion",
        description = "Solo puede decidir el responsable que continua asignado. Aprobar exige un horario completo " +
            "sin conflictos; rechazar exige comentario. Una decision exitosa es definitiva.",
    )
    @ApiResponse(responseCode = "200", description = "Solicitud revisada.", useReturnTypeSchema = true)
    @ApiResponse(
        responseCode = "400",
        description = "Decision, horario, lugar o comentario invalidos.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    @ApiResponse(
        responseCode = "404",
        description = "Solicitud, visita o peticion inexistente o combinacion incorrecta.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    @ApiResponse(
        responseCode = "409",
        description = "Peticion no revisable, trabajador distinto, modificacion concurrente o conflicto de agenda.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    fun review(
        @PathVariable applicationId: UUID,
        @PathVariable visitId: UUID,
        @PathVariable requestId: UUID,
        @RequestBody request: ReviewApplicationVisitRescheduleRequest,
    ): AdminApplicationVisitRescheduleRequestDetailResponse =
        service.review(applicationId, visitId, requestId, request)
}
