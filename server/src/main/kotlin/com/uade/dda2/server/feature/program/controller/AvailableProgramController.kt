package com.uade.dda2.server.feature.program.controller

import com.uade.dda2.server.feature.program.dto.available.response.AvailableProgramDetailResponse
import com.uade.dda2.server.feature.program.dto.available.response.AvailableProgramListResponse
import com.uade.dda2.server.feature.program.service.AvailableProgramService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/programs")
@Validated
@Tag(name = "Programas disponibles", description = "Consulta ciudadana de programas sociales vigentes o próximos.")
class AvailableProgramController(
    private val availableProgramService: AvailableProgramService,
) {

    @GetMapping
    @Operation(
        operationId = "listAvailablePrograms",
        summary = "Listar programas disponibles",
        description = "Devuelve los programas que poseen al menos una edición activa vigente o futura.",
    )
    fun list(
        @Parameter(description = "Número de página, comenzando en cero.", example = "0")
        @Min(value = 0, message = "La página no puede ser negativa.")
        @RequestParam(defaultValue = "0") page: Int,

        @Parameter(description = "Cantidad de elementos por página, entre 1 y 100.", example = "20")
        @Min(value = 1, message = "El tamaño de página debe ser mayor a cero.")
        @Max(value = 100, message = "El tamaño de página no puede superar 100 elementos.")
        @RequestParam(defaultValue = "20") size: Int,
    ): AvailableProgramListResponse =
        availableProgramService.list(
            page = page,
            size = size,
        )

    @GetMapping("/{id}")
    @Operation(
        operationId = "getAvailableProgram",
        summary = "Consultar un programa disponible",
        description = "Devuelve el programa con sus ediciones activas no finalizadas, beneficios, requisitos e incompatibilidades.",
    )
    fun get(
        @Parameter(description = "UUID del programa.", example = "550e8400-e29b-41d4-a716-446655440000")
        @PathVariable id: UUID,
    ): AvailableProgramDetailResponse =
        availableProgramService.get(id)
}
