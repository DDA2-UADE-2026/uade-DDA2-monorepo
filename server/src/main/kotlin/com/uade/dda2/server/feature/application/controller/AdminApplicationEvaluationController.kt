package com.uade.dda2.server.feature.application.controller

import com.uade.dda2.server.error.ErrorResponse
import com.uade.dda2.server.feature.application.dto.response.ApplicationEvaluationDetailResponse
import com.uade.dda2.server.feature.application.dto.response.ApplicationEvaluationListResponse
import com.uade.dda2.server.feature.application.entity.EvaluationPriority
import com.uade.dda2.server.feature.application.entity.VulnerabilityLevel
import com.uade.dda2.server.feature.application.service.APPLICATION_EVALUATION_VIEW_PERMISSION
import com.uade.dda2.server.feature.application.service.ApplicationEvaluationFilters
import com.uade.dda2.server.feature.application.service.ApplicationEvaluationService
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
@RequestMapping("/api/admin/application-evaluations", produces = ["application/json"])
@Validated
@Tag(name = "Evaluaciones sociales", description = "Registro y consulta administrativa de evaluaciones sociales.")
class AdminApplicationEvaluationController(private val service: ApplicationEvaluationService) {
    @GetMapping
    @PreAuthorize("hasAuthority('$APPLICATION_EVALUATION_VIEW_PERMISSION')")
    @Operation(
        operationId = "listApplicationEvaluations",
        summary = "Listar evaluaciones sociales",
        description = "Requiere applications:evaluations:view en el rol activo y vuelve a validar el permiso almacenado. " +
            "Todos los filtros son opcionales, el rango de fecha es inclusivo y el resultado se ordena por evaluatedAt descendente. " +
            "El listado omite la recomendacion sensible; se obtiene mediante el detalle.",
    )
    @ApiResponse(responseCode = "200", description = "Pagina filtrada de evaluaciones.", useReturnTypeSchema = true)
    @ApiResponse(
        responseCode = "400",
        description = "Paginacion, categoria, fecha o rango invalidos.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    fun list(
        @Parameter(description = "Numero de pagina, comenzando en cero.", example = "0")
        @Min(0) @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "Elementos por pagina, entre 1 y 100.", example = "20")
        @Min(1) @Max(100) @RequestParam(defaultValue = "20") size: Int,
        @Parameter(description = "ID exacto de la solicitud asociada.")
        @RequestParam(required = false) applicationId: UUID?,
        @Parameter(description = "Numero global exacto de la solicitud.", example = "15432")
        @Positive @RequestParam(required = false) applicationNumber: Long?,
        @Parameter(description = "ID interno exacto del titular.", example = "42")
        @Positive @RequestParam(required = false) applicantUserId: Long?,
        @Parameter(description = "ID interno exacto del profesional evaluador.", example = "7")
        @Positive @RequestParam(required = false) professionalId: Long?,
        @Parameter(description = "ID exacto del programa.")
        @RequestParam(required = false) programId: UUID?,
        @Parameter(description = "ID exacto de la edicion del programa.")
        @RequestParam(required = false) programEditionId: UUID?,
        @Parameter(description = "Nivel de vulnerabilidad exacto.", example = "HIGH")
        @RequestParam(required = false) vulnerability: VulnerabilityLevel?,
        @Parameter(description = "Prioridad exacta.", example = "URGENT")
        @RequestParam(required = false) priority: EvaluationPriority?,
        @Parameter(description = "Inicio inclusivo en ISO-8601 con offset.", example = "2026-10-01T00:00:00Z")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        @RequestParam(required = false) evaluatedFrom: OffsetDateTime?,
        @Parameter(description = "Fin inclusivo en ISO-8601 con offset.", example = "2026-10-31T23:59:59Z")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        @RequestParam(required = false) evaluatedTo: OffsetDateTime?,
    ): ApplicationEvaluationListResponse = service.list(
        page,
        size,
        ApplicationEvaluationFilters(
            applicationId = applicationId,
            applicationNumber = applicationNumber,
            applicantUserId = applicantUserId,
            professionalId = professionalId,
            programId = programId,
            programEditionId = programEditionId,
            vulnerability = vulnerability,
            priority = priority,
            evaluatedFrom = evaluatedFrom,
            evaluatedTo = evaluatedTo,
        ),
    )

    @GetMapping("/{evaluationId}")
    @PreAuthorize("hasAuthority('$APPLICATION_EVALUATION_VIEW_PERMISSION')")
    @Operation(
        operationId = "getApplicationEvaluation",
        summary = "Consultar una evaluacion social",
        description = "Requiere applications:evaluations:view en el rol activo. Devuelve la evaluacion completa, " +
            "incluida la recomendacion, el titular, el programa, la edicion y el profesional responsable.",
    )
    @ApiResponse(responseCode = "200", description = "Detalle completo de la evaluacion.", useReturnTypeSchema = true)
    @ApiResponse(
        responseCode = "404",
        description = "Evaluacion inexistente.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    fun get(@PathVariable evaluationId: UUID): ApplicationEvaluationDetailResponse = service.get(evaluationId)
}
