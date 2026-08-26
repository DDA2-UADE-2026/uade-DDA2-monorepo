package com.uade.dda2.server.feature.program.controller

import com.uade.dda2.server.feature.program.dto.admin.request.CreateProgramRequirementRequest
import com.uade.dda2.server.feature.program.dto.admin.request.UpdateProgramRequirementRequest
import com.uade.dda2.server.feature.program.dto.admin.response.ProgramRequirementResponse
import com.uade.dda2.server.feature.program.service.AdminProgramRequirementService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/admin/program-editions/{editionId}/requirements")
class AdminProgramRequirementController(
    private val adminProgramRequirementService: AdminProgramRequirementService,
) {

    @PreAuthorize("hasAuthority('programs:management:create')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @PathVariable editionId: UUID,
        @Valid @RequestBody request: CreateProgramRequirementRequest,
    ): ProgramRequirementResponse =
        adminProgramRequirementService.create(
            editionId = editionId,
            request = request,
        )

    @PreAuthorize("hasAuthority('programs:management:view')")
    @GetMapping
    fun findAll(
        @PathVariable editionId: UUID,
    ): List<ProgramRequirementResponse> =
        adminProgramRequirementService.list(
            editionId = editionId,
        )

    @PreAuthorize("hasAuthority('programs:management:view')")
    @GetMapping("/{requirementId}")
    fun findById(
        @PathVariable editionId: UUID,
        @PathVariable requirementId: UUID,
    ): ProgramRequirementResponse =
        adminProgramRequirementService.get(
            editionId = editionId,
            requirementId = requirementId,
        )

    @PreAuthorize("hasAuthority('programs:management:edit')")
    @PutMapping("/{requirementId}")
    fun update(
        @PathVariable editionId: UUID,
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
    fun delete(
        @PathVariable editionId: UUID,
        @PathVariable requirementId: UUID,
    ) {
        adminProgramRequirementService.delete(
            editionId = editionId,
            requirementId = requirementId,
        )
    }
}
