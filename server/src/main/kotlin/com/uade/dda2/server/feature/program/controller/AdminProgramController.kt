package com.uade.dda2.server.feature.program.controller

import com.uade.dda2.server.feature.program.dto.admin.request.CreateProgramRequest
import com.uade.dda2.server.feature.program.dto.admin.request.UpdateProgramRequest
import com.uade.dda2.server.feature.program.dto.admin.response.ProgramListResponse
import com.uade.dda2.server.feature.program.dto.admin.response.ProgramOptionResponse
import com.uade.dda2.server.feature.program.dto.admin.response.ProgramResponse
import com.uade.dda2.server.feature.program.service.AdminProgramService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/admin/programs")
@Validated
@Tag(name = "Programas", description = "Administración del catálogo de programas sociales.")
class AdminProgramController(
    private val adminProgramService: AdminProgramService,
) {

    @PreAuthorize("hasAuthority('programs:management:create')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear un programa", description = "Registra un nuevo programa social.")
    fun create(
        @Valid @RequestBody request: CreateProgramRequest,
    ): ProgramResponse =
        adminProgramService.create(request)

    @PreAuthorize("hasAuthority('programs:management:view')")
    @GetMapping
    @Operation(summary = "Listar programas", description = "Devuelve una página del catálogo de programas sociales.")
    fun list(
        @Parameter(description = "Número de página, comenzando en cero.", example = "0")
        @Min(value = 0, message = "La página no puede ser negativa.")
        @RequestParam(defaultValue = "0") page: Int,

        @Parameter(description = "Cantidad de elementos por página, entre 1 y 100.", example = "20")
        @Min(value = 1, message = "El tamaño de página debe ser mayor a cero.")
        @Max(value = 100, message = "El tamaño de página no puede superar 100 elementos.")
        @RequestParam(defaultValue = "20") size: Int,
    ): ProgramListResponse =
        adminProgramService.list(
            page = page,
            size = size,
        )

    @PreAuthorize("hasAuthority('programs:management:view')")
    @GetMapping("/options")
    @Operation(
        operationId = "listProgramOptions",
        summary = "Listar opciones de programas",
        description = "Devuelve una lista reducida de programas para controles de selección.",
    )
    fun options(): List<ProgramOptionResponse> =
        adminProgramService.options()

    @PreAuthorize("hasAuthority('programs:management:view')")
    @GetMapping("/{id}")
    @Operation(summary = "Consultar un programa", description = "Devuelve el detalle del programa identificado por su UUID.")
    fun findById(
        @Parameter(description = "UUID del programa.", example = "550e8400-e29b-41d4-a716-446655440000")
        @PathVariable id: UUID,
    ): ProgramResponse =
        adminProgramService.get(id)

    @PreAuthorize("hasAuthority('programs:management:edit')")
    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un programa", description = "Actualiza el nombre y el objetivo del programa indicado.")
    fun update(
        @Parameter(description = "UUID del programa.", example = "550e8400-e29b-41d4-a716-446655440000")
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateProgramRequest,
    ): ProgramResponse =
        adminProgramService.update(
            id = id,
            request = request,
        )

    @PreAuthorize("hasAuthority('programs:management:edit')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar un programa", description = "Elimina el programa si no existen relaciones que lo impidan.")
    fun delete(
        @Parameter(description = "UUID del programa.", example = "550e8400-e29b-41d4-a716-446655440000")
        @PathVariable id: UUID,
    ) {
        adminProgramService.delete(id)
    }
}
