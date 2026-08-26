package com.uade.dda2.server.feature.program.controller

import com.uade.dda2.server.feature.program.dto.admin.request.CreateProgramRequirementRequest
import com.uade.dda2.server.feature.program.dto.admin.request.UpdateProgramRequirementRequest
import com.uade.dda2.server.feature.program.dto.admin.response.ProgramRequirementResponse
import com.uade.dda2.server.feature.program.service.AdminProgramRequirementService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/admin/program-editions/{editionId}/requirements")
@Tag(name = "Requisitos de programas", description = "Administración de los requisitos exigidos por cada edición.")
class AdminProgramRequirementController(
    private val adminProgramRequirementService: AdminProgramRequirementService,
) {

    @PreAuthorize("hasAuthority('programs:management:create')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear un requisito", description = "Agrega un requisito a la edición indicada.")
    fun create(
        @Parameter(description = "UUID de la edición.")
        @PathVariable editionId: UUID,
        @Valid @RequestBody request: CreateProgramRequirementRequest,
    ): ProgramRequirementResponse =
        adminProgramRequirementService.create(
            editionId = editionId,
            request = request,
        )

    @PreAuthorize("hasAuthority('programs:management:view')")
    @GetMapping
    @Operation(summary = "Listar requisitos", description = "Devuelve todos los requisitos de la edición indicada.")
    fun findAll(
        @Parameter(description = "UUID de la edición.")
        @PathVariable editionId: UUID,
    ): List<ProgramRequirementResponse> =
        adminProgramRequirementService.list(
            editionId = editionId,
        )

    @PreAuthorize("hasAuthority('programs:management:view')")
    @GetMapping("/{requirementId}")
    @Operation(summary = "Consultar un requisito", description = "Devuelve un requisito perteneciente a la edición indicada.")
    fun findById(
        @Parameter(description = "UUID de la edición.")
        @PathVariable editionId: UUID,
        @Parameter(description = "UUID del requisito.")
        @PathVariable requirementId: UUID,
    ): ProgramRequirementResponse =
        adminProgramRequirementService.get(
            editionId = editionId,
            requirementId = requirementId,
        )

    @PreAuthorize("hasAuthority('programs:management:edit')")
    @PutMapping("/{requirementId}")
    @Operation(summary = "Actualizar un requisito", description = "Actualiza los datos del requisito indicado.")
    fun update(
        @Parameter(description = "UUID de la edición.")
        @PathVariable editionId: UUID,
        @Parameter(description = "UUID del requisito.")
        @PathVariable requirementId: UUID,
        @Valid @RequestBody request: UpdateProgramRequirementRequest,
    ): ProgramRequirementResponse =
        adminProgramRequirementService.update(
            editionId = editionId,
            requirementId = requirementId,
            request = request,
        )

    @PreAuthorize("hasAuthority('programs:management:edit')")
    @DeleteMapping("/{requirementId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar un requisito", description = "Elimina el requisito indicado de la edición.")
    fun delete(
        @Parameter(description = "UUID de la edición.")
        @PathVariable editionId: UUID,
        @Parameter(description = "UUID del requisito.")
        @PathVariable requirementId: UUID,
    ) {
        adminProgramRequirementService.delete(
            editionId = editionId,
            requirementId = requirementId,
        )
    }
}
