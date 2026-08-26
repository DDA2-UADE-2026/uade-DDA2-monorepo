package com.uade.dda2.server.feature.program.controller

import com.uade.dda2.server.feature.program.dto.admin.request.CreateProgramBenefitRequest
import com.uade.dda2.server.feature.program.dto.admin.request.UpdateProgramBenefitRequest
import com.uade.dda2.server.feature.program.dto.admin.response.ProgramBenefitResponse
import com.uade.dda2.server.feature.program.service.AdminProgramBenefitService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/admin/program-editions/{editionId}/benefits")
class AdminProgramBenefitController(
    private val adminProgramBenefitService: AdminProgramBenefitService,
) {

    @PreAuthorize("hasAuthority('programs:management:create')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @PathVariable editionId: UUID,
        @Valid @RequestBody request: CreateProgramBenefitRequest,
    ): ProgramBenefitResponse =
        adminProgramBenefitService.create(
            editionId = editionId,
            request = request,
        )

    @PreAuthorize("hasAuthority('programs:management:view')")
    @GetMapping
    fun findAll(
        @PathVariable editionId: UUID,
    ): List<ProgramBenefitResponse> =
        adminProgramBenefitService.list(
            editionId = editionId,
        )

    @PreAuthorize("hasAuthority('programs:management:view')")
    @GetMapping("/{benefitId}")
    fun findById(
        @PathVariable editionId: UUID,
        @PathVariable benefitId: UUID,
    ): ProgramBenefitResponse =
        adminProgramBenefitService.get(
            editionId = editionId,
            benefitId = benefitId,
        )

    @PreAuthorize("hasAuthority('programs:management:edit')")
    @PutMapping("/{benefitId}")
    fun update(
        @PathVariable editionId: UUID,
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
    fun delete(
        @PathVariable editionId: UUID,
        @PathVariable benefitId: UUID,
    ) {
        adminProgramBenefitService.delete(
            editionId = editionId,
            benefitId = benefitId,
        )
    }
}
