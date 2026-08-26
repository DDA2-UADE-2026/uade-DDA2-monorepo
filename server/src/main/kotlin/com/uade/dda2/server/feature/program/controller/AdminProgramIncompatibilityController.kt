package com.uade.dda2.server.feature.program.controller

import com.uade.dda2.server.feature.program.dto.admin.response.ProgramIncompatibilityResponse
import com.uade.dda2.server.feature.program.service.AdminProgramIncompatibilityService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/admin/programs/{programId}/incompatibilities")
@Tag(name = "Incompatibilidades de programas", description = "Administración de relaciones de incompatibilidad entre programas.")
class AdminProgramIncompatibilityController(
    private val adminProgramIncompatibilityService: AdminProgramIncompatibilityService,
) {

    @PreAuthorize("hasAuthority('programs:management:view')")
    @GetMapping
    @Operation(summary = "Listar incompatibilidades", description = "Devuelve los programas incompatibles con el programa indicado.")
    fun findAll(
        @Parameter(description = "UUID del programa.")
        @PathVariable programId: UUID,
    ): List<ProgramIncompatibilityResponse> =
        adminProgramIncompatibilityService.list(
            programId = programId,
        )

    @PreAuthorize("hasAuthority('programs:management:edit')")
    @PostMapping("/{incompatibleProgramId}")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear una incompatibilidad", description = "Declara que dos programas son incompatibles entre sí.")
    fun create(
        @Parameter(description = "UUID del programa de origen.")
        @PathVariable programId: UUID,
        @Parameter(description = "UUID del programa incompatible.")
        @PathVariable incompatibleProgramId: UUID,
    ): ProgramIncompatibilityResponse =
        adminProgramIncompatibilityService.create(
            programId = programId,
            incompatibleProgramId = incompatibleProgramId,
        )

    @PreAuthorize("hasAuthority('programs:management:edit')")
    @DeleteMapping("/{incompatibleProgramId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar una incompatibilidad", description = "Elimina la relación de incompatibilidad entre los programas indicados.")
    fun delete(
        @Parameter(description = "UUID del programa de origen.")
        @PathVariable programId: UUID,
        @Parameter(description = "UUID del programa incompatible.")
        @PathVariable incompatibleProgramId: UUID,
    ) {
        adminProgramIncompatibilityService.delete(
            programId = programId,
            incompatibleProgramId = incompatibleProgramId,
        )
    }
}
