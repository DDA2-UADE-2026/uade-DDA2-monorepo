package com.uade.dda2.server.feature.application.controller

import com.uade.dda2.server.error.ErrorResponse
import com.uade.dda2.server.feature.application.dto.response.ApplicationVisitDetailResponse
import com.uade.dda2.server.feature.application.dto.response.ApplicationVisitListResponse
import com.uade.dda2.server.feature.application.entity.ApplicationVisitStatus
import com.uade.dda2.server.feature.application.entity.ApplicationVisitType
import com.uade.dda2.server.feature.application.service.APPLICATION_VISIT_VIEW_PERMISSION
import com.uade.dda2.server.feature.application.service.ApplicationVisitFilters
import com.uade.dda2.server.feature.application.service.ApplicationVisitService
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
@RequestMapping("/api/admin/application-visits", produces = ["application/json"])
@Validated
@Tag(name = "Visitas sociales", description = "Agenda y resultados administrativos de visitas y entrevistas sociales.")
class AdminApplicationVisitController(private val service: ApplicationVisitService) {
    @GetMapping
    @PreAuthorize("hasAuthority('$APPLICATION_VISIT_VIEW_PERMISSION')")
    @Operation(
        operationId = "listApplicationVisits",
        summary = "Listar visitas y entrevistas sociales",
        description = "Requiere applications:visits:view en el rol activo. Los filtros son opcionales; la ventana " +
            "temporal usa intervalos semiabiertos y devuelve actuaciones que se superponen con ella. " +
            "El resultado se ordena por startsAt ascendente.",
    )
    @ApiResponse(responseCode = "200", description = "Pagina filtrada de actuaciones sociales.", useReturnTypeSchema = true)
    @ApiResponse(
        responseCode = "400",
        description = "Paginacion, tipo, estado, fecha o ventana invalidos.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    fun list(
        @Parameter(description = "Numero de pagina, comenzando en cero.", example = "0")
        @Min(0) @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "Elementos por pagina, entre 1 y 100.", example = "20")
        @Min(1) @Max(100) @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) applicationId: UUID?,
        @Positive @RequestParam(required = false) applicationNumber: Long?,
        @Positive @RequestParam(required = false) applicantUserId: Long?,
        @Positive @RequestParam(required = false) responsibleId: Long?,
        @RequestParam(required = false) programId: UUID?,
        @RequestParam(required = false) programEditionId: UUID?,
        @Parameter(description = "Tipo exacto de actuacion.", example = "VISIT")
        @RequestParam(required = false) type: ApplicationVisitType?,
        @Parameter(description = "Estado exacto: SCHEDULED, CANCELLED, REALIZED o NOT_REALIZED.", example = "SCHEDULED")
        @RequestParam(required = false) status: ApplicationVisitStatus?,
        @Parameter(description = "Inicio inclusivo de la ventana ISO-8601 con offset.", example = "2026-10-20T00:00:00-03:00")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        @RequestParam(required = false) windowStart: OffsetDateTime?,
        @Parameter(description = "Fin exclusivo de la ventana ISO-8601 con offset.", example = "2026-10-21T00:00:00-03:00")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        @RequestParam(required = false) windowEnd: OffsetDateTime?,
    ): ApplicationVisitListResponse = service.list(
        page,
        size,
        ApplicationVisitFilters(
            applicationId = applicationId,
            applicationNumber = applicationNumber,
            applicantUserId = applicantUserId,
            responsibleId = responsibleId,
            programId = programId,
            programEditionId = programEditionId,
            type = type,
            status = status,
            windowStart = windowStart,
            windowEnd = windowEnd,
        ),
    )

    @GetMapping("/{visitId}")
    @PreAuthorize("hasAuthority('$APPLICATION_VISIT_VIEW_PERMISSION')")
    @Operation(
        operationId = "getApplicationVisit",
        summary = "Consultar una visita o entrevista social",
        description = "Requiere applications:visits:view en el rol activo y devuelve la actuacion, su resultado cuando existe y el contexto completo de la solicitud.",
    )
    @ApiResponse(responseCode = "200", description = "Detalle completo de la actuacion.", useReturnTypeSchema = true)
    @ApiResponse(
        responseCode = "404",
        description = "Visita o entrevista inexistente.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    fun get(@PathVariable visitId: UUID): ApplicationVisitDetailResponse = service.get(visitId)
}
