package com.uade.dda2.server.feature.application.controller

import com.uade.dda2.server.error.ErrorResponse
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
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/applications/visits", produces = ["application/json"])
@Validated
@Tag(
    name = "Visitas propias",
    description = "Consulta ciudadana de visitas y entrevistas asociadas a solicitudes propias.",
)
class CitizenVisitController(private val service: ApplicationVisitService) {
    @GetMapping
    @PreAuthorize("hasAuthority('$APPLICATION_OWN_VISIT_VIEW_PERMISSION')")
    @Operation(
        operationId = "listAllOwnVisits",
        summary = "Listar visitas y entrevistas de todas las solicitudes propias",
        description = "Requiere applications:own:visits:view en el rol activo. La identidad se obtiene del JWT. " +
            "Con upcoming=true devuelve solo actuaciones programadas que aun no terminaron, ordenadas por inicio " +
            "ascendente; si no, todas ordenadas por inicio descendente.",
    )
    @ApiResponse(responseCode = "200", description = "Pagina de actuaciones propias.", useReturnTypeSchema = true)
    @ApiResponse(
        responseCode = "400",
        description = "Paginacion invalida.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
    )
    fun list(
        @Parameter(description = "Solo actuaciones programadas y vigentes.", example = "true")
        @RequestParam(defaultValue = "false") upcoming: Boolean,
        @Parameter(description = "Numero de pagina, comenzando en cero.", example = "0")
        @Min(0) @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "Elementos por pagina, entre 1 y 100.", example = "20")
        @Min(1) @Max(100) @RequestParam(defaultValue = "20") size: Int,
    ): CitizenApplicationVisitListResponse = service.listAllOwn(upcoming, page, size)
}
