package com.uade.dda2.server.feature.application.controller

import com.uade.dda2.server.error.ErrorResponse
import com.uade.dda2.server.feature.application.dto.request.CreateApplicationEvaluationRequest
import com.uade.dda2.server.feature.application.dto.response.ApplicationEvaluationResponse
import com.uade.dda2.server.feature.application.service.APPLICATION_EVALUATION_CREATE_PERMISSION
import com.uade.dda2.server.feature.application.service.ApplicationEvaluationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/applications/{applicationId}/evaluation", produces = ["application/json"])
@Tag(name = "Evaluaciones sociales", description = "Registro profesional de vulnerabilidad y prioridad de solicitudes.")
class ApplicationEvaluationController(private val service: ApplicationEvaluationService) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('$APPLICATION_EVALUATION_CREATE_PERMISSION')")
    @Operation(
        operationId = "createApplicationEvaluation",
        summary = "Registrar la evaluacion social de una solicitud",
        description = "Requiere applications:evaluations:create en el rol activo. Registra una unica evaluacion, " +
            "asigna la solicitud al usuario autenticado y cambia su estado a IN_EVALUATION.",
    )
    @ApiResponse(responseCode = "201", description = "Evaluacion registrada.", useReturnTypeSchema = true)
    @ApiResponse(
        responseCode = "400",
        description = "Categorias, recomendacion o cuerpo invalidos.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    @ApiResponse(
        responseCode = "404",
        description = "Solicitud inexistente.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    @ApiResponse(
        responseCode = "409",
        description = "La solicitud ya fue evaluada, no admite evaluacion o pertenece a otro trabajador.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    fun create(
        @PathVariable applicationId: UUID,
        @RequestBody request: CreateApplicationEvaluationRequest,
    ): ApplicationEvaluationResponse = service.create(applicationId, request)
}
