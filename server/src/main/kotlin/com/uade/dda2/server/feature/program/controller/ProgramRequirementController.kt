package com.uade.dda2.server.feature.program.controller

import com.uade.dda2.server.feature.program.dto.request.CreateProgramRequirementRequest
import com.uade.dda2.server.feature.program.dto.request.UpdateProgramRequirementRequest
import com.uade.dda2.server.feature.program.dto.response.ProgramRequirementResponse
import com.uade.dda2.server.feature.program.service.ProgramRequirementService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/program-editions/{editionId}/requirements")
class ProgramRequirementController(
    private val programRequirementService: ProgramRequirementService,
) {

    @PreAuthorize("hasAuthority('programs:create')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @PathVariable editionId: UUID,
        @Valid @RequestBody request: CreateProgramRequirementRequest,
    ): ProgramRequirementResponse =
        programRequirementService.create(
            editionId = editionId,
            request = request,
        )

    @PreAuthorize("hasAuthority('programs:view')")
    @GetMapping
    fun findAll(
        @PathVariable editionId: UUID,
    ): List<ProgramRequirementResponse> =
        programRequirementService.list(
            editionId = editionId,
        )

    @PreAuthorize("hasAuthority('programs:view')")
    @GetMapping("/{requirementId}")
    fun findById(
        @PathVariable editionId: UUID,
        @PathVariable requirementId: UUID,
    ): ProgramRequirementResponse =
        programRequirementService.get(
            editionId = editionId,
            requirementId = requirementId,
        )

    @PreAuthorize("hasAuthority('programs:edit')")
    @PutMapping("/{requirementId}")
    fun update(
        @PathVariable editionId: UUID,
        @PathVariable requirementId: UUID,
        @Valid @RequestBody request: UpdateProgramRequirementRequest,
    ): ProgramRequirementResponse =
        programRequirementService.update(
            editionId = editionId,
            requirementId = requirementId,
            request = request,
        )

    @PreAuthorize("hasAuthority('programs:delete')")
    @DeleteMapping("/{requirementId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @PathVariable editionId: UUID,
        @PathVariable requirementId: UUID,
    ) {
        programRequirementService.delete(
            editionId = editionId,
            requirementId = requirementId,
        )
    }
}