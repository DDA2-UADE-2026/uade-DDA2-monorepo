package com.uade.dda2.server.feature.program.controller

import com.uade.dda2.server.feature.program.dto.admin.request.CreateProgramEditionRequest
import com.uade.dda2.server.feature.program.dto.admin.request.UpdateProgramEditionRequest
import com.uade.dda2.server.feature.program.dto.admin.response.ProgramEditionListResponse
import com.uade.dda2.server.feature.program.dto.admin.response.ProgramEditionOptionResponse
import com.uade.dda2.server.feature.program.dto.admin.response.ProgramEditionResponse
import com.uade.dda2.server.feature.program.service.AdminProgramEditionService
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
@RequestMapping("/api/admin/program-editions")
@Validated
@Tag(name = "Ediciones de programas", description = "Administración y ciclo de vida de las ediciones de programas sociales.")
class AdminProgramEditionController(
    private val adminProgramEditionService: AdminProgramEditionService,
) {

    @PreAuthorize("hasAuthority('programs:management:create')")
    @PostMapping("/program/{programId}")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear una edición", description = "Crea una edición para el programa indicado.")
    fun create(
        @Parameter(description = "UUID del programa.")
        @PathVariable programId: UUID,
        @Valid @RequestBody request: CreateProgramEditionRequest,
    ): ProgramEditionResponse =
        adminProgramEditionService.create(
            programId = programId,
            request = request,
        )

    @PreAuthorize("hasAuthority('programs:management:view')")
    @GetMapping("/program/{programId}")
    @Operation(summary = "Listar ediciones", description = "Devuelve una página de ediciones pertenecientes a un programa.")
    fun list(
        @Parameter(description = "UUID del programa.")
        @PathVariable programId: UUID,

        @Parameter(description = "Número de página, comenzando en cero.", example = "0")
        @Min(value = 0, message = "La página no puede ser negativa.")
        @RequestParam(defaultValue = "0") page: Int,

        @Parameter(description = "Cantidad de elementos por página, entre 1 y 100.", example = "20")
        @Min(value = 1, message = "El tamaño de página debe ser mayor a cero.")
        @Max(value = 100, message = "El tamaño de página no puede superar 100 elementos.")
        @RequestParam(defaultValue = "20") size: Int,
    ): ProgramEditionListResponse =
        adminProgramEditionService.list(
            programId = programId,
            page = page,
            size = size,
        )

    @PreAuthorize("hasAuthority('programs:management:view')")
    @GetMapping("/program/{programId}/options")
    @Operation(summary = "Listar opciones de ediciones", description = "Devuelve las ediciones de un programa en formato reducido para controles de selección.")
    fun options(
        @Parameter(description = "UUID del programa.")
        @PathVariable programId: UUID,
    ): List<ProgramEditionOptionResponse> =
        adminProgramEditionService.options(programId)

    @PreAuthorize("hasAuthority('programs:management:view')")
    @GetMapping("/{id}")
    @Operation(summary = "Consultar una edición", description = "Devuelve el detalle de la edición indicada.")
    fun findById(
        @Parameter(description = "UUID de la edición.")
        @PathVariable id: UUID,
    ): ProgramEditionResponse =
        adminProgramEditionService.get(id)

    @PreAuthorize("hasAuthority('programs:management:edit')")
    @PutMapping("/{id}")
    @Operation(summary = "Actualizar una edición", description = "Actualiza los datos de la edición indicada.")
    fun update(
        @Parameter(description = "UUID de la edición.")
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateProgramEditionRequest,
    ): ProgramEditionResponse =
        adminProgramEditionService.update(
            id = id,
            request = request,
        )

    @PreAuthorize("hasAuthority('programs:management:edit')")
    @PatchMapping("/{id}/activate")
    @Operation(summary = "Activar una edición", description = "Cambia la edición al estado activo cuando la transición es válida.")
    fun activate(
        @Parameter(description = "UUID de la edición.")
        @PathVariable id: UUID,
    ): ProgramEditionResponse =
        adminProgramEditionService.activate(id)

    @PreAuthorize("hasAuthority('programs:management:edit')")
    @PatchMapping("/{id}/suspend")
    @Operation(summary = "Suspender una edición", description = "Cambia la edición al estado suspendido cuando la transición es válida.")
    fun suspend(
        @Parameter(description = "UUID de la edición.")
        @PathVariable id: UUID,
    ): ProgramEditionResponse =
        adminProgramEditionService.suspend(id)

    @PreAuthorize("hasAuthority('programs:management:edit')")
    @PatchMapping("/{id}/close")
    @Operation(summary = "Cerrar una edición", description = "Cambia la edición al estado cerrado cuando la transición es válida.")
    fun close(
        @Parameter(description = "UUID de la edición.")
        @PathVariable id: UUID,
    ): ProgramEditionResponse =
        adminProgramEditionService.close(id)

    @PreAuthorize("hasAuthority('programs:management:edit')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar una edición", description = "Elimina la edición si su estado y sus relaciones lo permiten.")
    fun delete(
        @Parameter(description = "UUID de la edición.")
        @PathVariable id: UUID,
    ) {
        adminProgramEditionService.delete(id)
    }
}
