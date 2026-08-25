package com.uade.dda2.server.feature.program.controller

import com.uade.dda2.server.feature.program.dto.request.CreateProgramBenefitRequest
import com.uade.dda2.server.feature.program.dto.request.UpdateProgramBenefitRequest
import com.uade.dda2.server.feature.program.dto.response.ProgramBenefitResponse
import com.uade.dda2.server.feature.program.service.ProgramBenefitService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/program-editions/{editionId}/benefits")
class ProgramBenefitController(
    private val programBenefitService: ProgramBenefitService,
) {

    @PreAuthorize("hasAuthority('programs:create')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @PathVariable editionId: UUID,
        @Valid @RequestBody request: CreateProgramBenefitRequest,
    ): ProgramBenefitResponse =
        programBenefitService.create(
            editionId = editionId,
            request = request,
        )

    @PreAuthorize("hasAuthority('programs:view')")
    @GetMapping
    fun findAll(
        @PathVariable editionId: UUID,
    ): List<ProgramBenefitResponse> =
        programBenefitService.list(
            editionId = editionId,
        )

    @PreAuthorize("hasAuthority('programs:view')")
    @GetMapping("/{benefitId}")
    fun findById(
        @PathVariable editionId: UUID,
        @PathVariable benefitId: UUID,
    ): ProgramBenefitResponse =
        programBenefitService.get(
            editionId = editionId,
            benefitId = benefitId,
        )

    @PreAuthorize("hasAuthority('programs:edit')")
    @PutMapping("/{benefitId}")
    fun update(
        @PathVariable editionId: UUID,
        @PathVariable benefitId: UUID,
        @Valid @RequestBody request: UpdateProgramBenefitRequest,
    ): ProgramBenefitResponse =
        programBenefitService.update(
            editionId = editionId,
            benefitId = benefitId,
            request = request,
        )

    @PreAuthorize("hasAuthority('programs:delete')")
    @DeleteMapping("/{benefitId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @PathVariable editionId: UUID,
        @PathVariable benefitId: UUID,
    ) {
        programBenefitService.delete(
            editionId = editionId,
            benefitId = benefitId,
        )
    }
}