package com.uade.dda2.server.feature.program.controller

import com.uade.dda2.server.feature.program.dto.admin.request.CreateProgramBenefitRequest
import com.uade.dda2.server.feature.program.dto.admin.request.UpdateProgramBenefitRequest
import com.uade.dda2.server.feature.program.dto.admin.response.ProgramBenefitResponse
import com.uade.dda2.server.feature.program.service.AdminProgramBenefitService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/admin/program-editions/{editionId}/benefits")
@Tag(name = "Beneficios de programas", description = "Administración de los beneficios ofrecidos por cada edición.")
class AdminProgramBenefitController(
    private val adminProgramBenefitService: AdminProgramBenefitService,
) {

    @PreAuthorize("hasAuthority('programs:management:create')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear un beneficio", description = "Agrega un beneficio a la edición indicada.")
    fun create(
        @Parameter(description = "UUID de la edición.")
        @PathVariable editionId: UUID,
        @Valid @RequestBody request: CreateProgramBenefitRequest,
    ): ProgramBenefitResponse =
        adminProgramBenefitService.create(
            editionId = editionId,
            request = request,
        )

    @PreAuthorize("hasAuthority('programs:management:view')")
    @GetMapping
    @Operation(summary = "Listar beneficios", description = "Devuelve todos los beneficios de la edición indicada.")
    fun findAll(
        @Parameter(description = "UUID de la edición.")
        @PathVariable editionId: UUID,
    ): List<ProgramBenefitResponse> =
        adminProgramBenefitService.list(
            editionId = editionId,
        )

    @PreAuthorize("hasAuthority('programs:management:view')")
    @GetMapping("/{benefitId}")
    @Operation(summary = "Consultar un beneficio", description = "Devuelve un beneficio perteneciente a la edición indicada.")
    fun findById(
        @Parameter(description = "UUID de la edición.")
        @PathVariable editionId: UUID,
        @Parameter(description = "UUID del beneficio.")
        @PathVariable benefitId: UUID,
    ): ProgramBenefitResponse =
        adminProgramBenefitService.get(
            editionId = editionId,
            benefitId = benefitId,
        )

    @PreAuthorize("hasAuthority('programs:management:edit')")
    @PutMapping("/{benefitId}")
    @Operation(summary = "Actualizar un beneficio", description = "Actualiza los datos del beneficio indicado.")
    fun update(
        @Parameter(description = "UUID de la edición.")
        @PathVariable editionId: UUID,
        @Parameter(description = "UUID del beneficio.")
        @PathVariable benefitId: UUID,
        @Valid @RequestBody request: UpdateProgramBenefitRequest,
    ): ProgramBenefitResponse =
        adminProgramBenefitService.update(
            editionId = editionId,
            benefitId = benefitId,
            request = request,
        )

    @PreAuthorize("hasAuthority('programs:management:edit')")
    @DeleteMapping("/{benefitId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar un beneficio", description = "Elimina el beneficio indicado de la edición.")
    fun delete(
        @Parameter(description = "UUID de la edición.")
        @PathVariable editionId: UUID,
        @Parameter(description = "UUID del beneficio.")
        @PathVariable benefitId: UUID,
    ) {
        adminProgramBenefitService.delete(
            editionId = editionId,
            benefitId = benefitId,
        )
    }
}
