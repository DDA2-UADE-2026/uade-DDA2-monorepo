package com.uade.dda2.server.feature.indicator.controller

import com.uade.dda2.server.error.ErrorResponse
import com.uade.dda2.server.feature.indicator.dto.IndicatorsResponse
import com.uade.dda2.server.feature.indicator.service.INDICATORS_VIEW_PERMISSION
import com.uade.dda2.server.feature.indicator.service.IndicatorService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/api/admin/indicators", produces = ["application/json"])
@Tag(name = "Indicadores", description = "Tablero de indicadores de cobertura, cupos, evaluación, visitas y resultados.")
class IndicatorController(private val service: IndicatorService) {
    @GetMapping
    @PreAuthorize("hasAuthority('$INDICATORS_VIEW_PERMISSION')")
    @Operation(operationId = "getIndicators", summary = "Consultar el tablero de indicadores", description =
        "Requiere indicators:view en el rol activo. Devuelve todos los indicadores agregados en el servidor. " +
        "El período (from/to, ambos inclusive, en la zona horaria municipal) aplica a presentaciones, resoluciones, " +
        "evaluaciones, visitas, documentos, turnos y actividades; cupos, carga de trabajo y casos pendientes reflejan " +
        "el estado actual. programId restringe lo relativo a solicitudes; turnos y actividades no dependen del programa. " +
        "Sin fechas, toma los últimos 90 días. El período admite hasta 1100 días.")
    @ApiResponse(responseCode = "200", description = "Indicadores del período.", useReturnTypeSchema = true)
    @ApiResponse(responseCode = "400", description = "Fechas inválidas, invertidas o período demasiado largo.", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @ApiResponse(responseCode = "401", description = "Sin JWT de acceso válido o usuario inexistente/inactivo.", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    @ApiResponse(responseCode = "403", description = "Sin permiso en el rol activo o rol retirado.", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    fun indicators(
        @Parameter(description = "Inicio del período, inclusive (ISO-8601).", example = "2026-08-01")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate?,
        @Parameter(description = "Fin del período, inclusive (ISO-8601).", example = "2026-10-31")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate?,
        @Parameter(description = "Opcional. Restringe los indicadores de solicitudes a un programa.")
        @RequestParam(required = false) programId: UUID?,
    ): IndicatorsResponse = service.indicators(from, to, programId)
}
