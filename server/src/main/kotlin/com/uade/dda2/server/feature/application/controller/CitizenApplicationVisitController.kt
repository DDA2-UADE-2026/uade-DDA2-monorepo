package com.uade.dda2.server.feature.application.controller

import com.uade.dda2.server.error.ErrorResponse
import com.uade.dda2.server.feature.application.dto.response.CitizenApplicationVisitDetailResponse
import com.uade.dda2.server.feature.application.dto.response.CitizenApplicationVisitListResponse
import com.uade.dda2.server.feature.application.service.APPLICATION_OWN_VISIT_VIEW_PERMISSION
import com.uade.dda2.server.feature.application.service.ApplicationVisitService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/applications/{applicationId}/visits", produces = ["application/json"])
@Validated
@Tag(
    name = "Visitas propias",
    description = "Consulta ciudadana de visitas y entrevistas asociadas a solicitudes propias.",
)
class CitizenApplicationVisitController(private val service: ApplicationVisitService) {
    @GetMapping
    @PreAuthorize("hasAuthority('$APPLICATION_OWN_VISIT_VIEW_PERMISSION')")
    @Operation(
        operationId = "listOwnApplicationVisits",
        summary = "Listar visitas y entrevistas de una solicitud propia",
        description = "Requiere applications:own:visits:view en el rol activo. La identidad se obtiene del JWT; " +
            "una solicitud ajena se responde como inexistente. Incluye todos los estados y ordena por inicio descendente.",
    )
    @ApiResponse(responseCode = "200", description = "Pagina de actuaciones propias.", useReturnTypeSchema = true)
    @ApiResponse(
        responseCode = "400",
        description = "Paginacion invalida.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    @ApiResponse(
        responseCode = "404",
        description = "Solicitud inexistente o perteneciente a otro usuario.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    fun list(
        @PathVariable applicationId: UUID,
        @Parameter(description = "Numero de pagina, comenzando en cero.", example = "0")
        @Min(0) @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "Elementos por pagina, entre 1 y 100.", example = "20")
        @Min(1) @Max(100) @RequestParam(defaultValue = "20") size: Int,
    ): CitizenApplicationVisitListResponse = service.listOwn(applicationId, page, size)

    @GetMapping("/{visitId}")
    @PreAuthorize("hasAuthority('$APPLICATION_OWN_VISIT_VIEW_PERMISSION')")
    @Operation(
        operationId = "getOwnApplicationVisit",
        summary = "Consultar una visita o entrevista de una solicitud propia",
        description = "Requiere applications:own:visits:view en el rol activo. Devuelve el horario vigente y, " +
            "si fue cancelada, el motivo. Nunca expone observaciones ni conclusiones sociales.",
    )
    @ApiResponse(responseCode = "200", description = "Detalle ciudadano de la actuacion.", useReturnTypeSchema = true)
    @ApiResponse(
        responseCode = "404",
        description = "Solicitud o actuacion inexistente, ajena o asociada a otra solicitud.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    fun get(
        @PathVariable applicationId: UUID,
        @PathVariable visitId: UUID,
    ): CitizenApplicationVisitDetailResponse = service.getOwn(applicationId, visitId)
}
