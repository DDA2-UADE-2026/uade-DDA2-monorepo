package com.uade.dda2.server.feature.application.controller

import com.uade.dda2.server.error.ErrorResponse
import com.uade.dda2.server.feature.application.dto.response.AdminApplicationVisitRescheduleRequestDetailResponse
import com.uade.dda2.server.feature.application.dto.response.AdminApplicationVisitRescheduleRequestListResponse
import com.uade.dda2.server.feature.application.entity.ApplicationVisitRescheduleRequestStatus
import com.uade.dda2.server.feature.application.service.APPLICATION_VISIT_RESCHEDULE_REQUEST_VIEW_PERMISSION
import com.uade.dda2.server.feature.application.service.ApplicationVisitRescheduleRequestFilters
import com.uade.dda2.server.feature.application.service.ApplicationVisitRescheduleRequestService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Positive
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime
import java.util.UUID

@RestController
@RequestMapping("/api/admin/application-visit-reschedule-requests", produces = ["application/json"])
@Validated
@Tag(
    name = "Solicitudes de reprogramacion",
    description = "Bandeja administrativa de solicitudes ciudadanas de reprogramacion.",
)
class AdminApplicationVisitRescheduleRequestController(
    private val service: ApplicationVisitRescheduleRequestService,
) {
    @GetMapping
    @PreAuthorize("hasAuthority('$APPLICATION_VISIT_RESCHEDULE_REQUEST_VIEW_PERMISSION')")
    @Operation(
        operationId = "listApplicationVisitRescheduleRequests",
        summary = "Listar solicitudes de reprogramacion",
        description = "Aplica filtros opcionales y ordena por antiguedad ascendente. El listado no expone motivo, " +
            "comentario ni lugares.",
    )
    @ApiResponse(responseCode = "200", description = "Pagina filtrada de solicitudes.", useReturnTypeSchema = true)
    @ApiResponse(
        responseCode = "400",
        description = "Paginacion, estado o ventana de solicitud invalidos.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    fun list(
        @Min(0) @RequestParam(defaultValue = "0") page: Int,
        @Min(1) @Max(100) @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) status: ApplicationVisitRescheduleRequestStatus?,
        @RequestParam(required = false) applicationId: UUID?,
        @Positive @RequestParam(required = false) applicationNumber: Long?,
        @RequestParam(required = false) visitId: UUID?,
        @Positive @RequestParam(required = false) applicantUserId: Long?,
        @Positive @RequestParam(required = false) responsibleId: Long?,
        @RequestParam(required = false) programId: UUID?,
        @RequestParam(required = false) programEditionId: UUID?,
        @Parameter(description = "Inicio inclusivo de la ventana de solicitud.")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        @RequestParam(required = false) requestedFrom: OffsetDateTime?,
        @Parameter(description = "Fin exclusivo de la ventana de solicitud.")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        @RequestParam(required = false) requestedTo: OffsetDateTime?,
    ): AdminApplicationVisitRescheduleRequestListResponse = service.listAdmin(
        page,
        size,
        ApplicationVisitRescheduleRequestFilters(
            status = status,
            applicationId = applicationId,
            applicationNumber = applicationNumber,
            visitId = visitId,
            applicantUserId = applicantUserId,
            responsibleId = responsibleId,
            programId = programId,
            programEditionId = programEditionId,
            requestedFrom = requestedFrom,
            requestedTo = requestedTo,
        ),
    )

    @GetMapping("/{requestId}")
    @PreAuthorize("hasAuthority('$APPLICATION_VISIT_RESCHEDULE_REQUEST_VIEW_PERMISSION')")
    @Operation(
        operationId = "getApplicationVisitRescheduleRequest",
        summary = "Consultar una solicitud de reprogramacion",
        description = "Devuelve el motivo, comentario y contexto completo solo a usuarios autorizados.",
    )
    @ApiResponse(responseCode = "200", description = "Detalle administrativo.", useReturnTypeSchema = true)
    @ApiResponse(
        responseCode = "404",
        description = "Solicitud de reprogramacion inexistente.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    fun get(@PathVariable requestId: UUID): AdminApplicationVisitRescheduleRequestDetailResponse =
        service.getAdmin(requestId)
}
