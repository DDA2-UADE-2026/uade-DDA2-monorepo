package com.uade.dda2.server.feature.program.controller

import com.uade.dda2.server.feature.program.dto.response.ProgramIncompatibilityResponse
import com.uade.dda2.server.feature.program.service.ProgramIncompatibilityService
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/programs/{programId}/incompatibilities")
class ProgramIncompatibilityController(
    private val programIncompatibilityService: ProgramIncompatibilityService,
) {

    @PreAuthorize("hasAuthority('programs:view')")
    @GetMapping
    fun findAll(
        @PathVariable programId: UUID,
    ): List<ProgramIncompatibilityResponse> =
        programIncompatibilityService.list(
            programId = programId,
        )

    @PreAuthorize("hasAuthority('programs:edit')")
    @PostMapping("/{incompatibleProgramId}")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @PathVariable programId: UUID,
        @PathVariable incompatibleProgramId: UUID,
    ): ProgramIncompatibilityResponse =
        programIncompatibilityService.create(
            programId = programId,
            incompatibleProgramId = incompatibleProgramId,
        )

    @PreAuthorize("hasAuthority('programs:edit')")
    @DeleteMapping("/{incompatibleProgramId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @PathVariable programId: UUID,
        @PathVariable incompatibleProgramId: UUID,
    ) {
        programIncompatibilityService.delete(
            programId = programId,
            incompatibleProgramId = incompatibleProgramId,
        )
    }
}