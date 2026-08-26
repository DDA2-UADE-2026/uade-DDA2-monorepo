package com.uade.dda2.server.feature.program.controller

import com.uade.dda2.server.feature.program.dto.admin.response.ProgramIncompatibilityResponse
import com.uade.dda2.server.feature.program.service.AdminProgramIncompatibilityService
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/admin/programs/{programId}/incompatibilities")
class AdminProgramIncompatibilityController(
    private val adminProgramIncompatibilityService: AdminProgramIncompatibilityService,
) {

    @PreAuthorize("hasAuthority('programs:management:view')")
    @GetMapping
    fun findAll(
        @PathVariable programId: UUID,
    ): List<ProgramIncompatibilityResponse> =
        adminProgramIncompatibilityService.list(
            programId = programId,
        )

    @PreAuthorize("hasAuthority('programs:management:edit')")
    @PostMapping("/{incompatibleProgramId}")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @PathVariable programId: UUID,
        @PathVariable incompatibleProgramId: UUID,
    ): ProgramIncompatibilityResponse =
        adminProgramIncompatibilityService.create(
            programId = programId,
            incompatibleProgramId = incompatibleProgramId,
        )

    @PreAuthorize("hasAuthority('programs:management:edit')")
    @DeleteMapping("/{incompatibleProgramId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @PathVariable programId: UUID,
        @PathVariable incompatibleProgramId: UUID,
    ) {
        adminProgramIncompatibilityService.delete(
            programId = programId,
            incompatibleProgramId = incompatibleProgramId,
        )
    }
}
