package com.uade.dda2.server.feature.program.controller

import com.uade.dda2.server.feature.program.dto.admin.request.CreateProgramEditionRequest
import com.uade.dda2.server.feature.program.dto.admin.request.UpdateProgramEditionRequest
import com.uade.dda2.server.feature.program.dto.admin.response.ProgramEditionListResponse
import com.uade.dda2.server.feature.program.dto.admin.response.ProgramEditionOptionResponse
import com.uade.dda2.server.feature.program.dto.admin.response.ProgramEditionResponse
import com.uade.dda2.server.feature.program.service.AdminProgramEditionService
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
class AdminProgramEditionController(
    private val adminProgramEditionService: AdminProgramEditionService,
) {

    @PreAuthorize("hasAuthority('programs:management:create')")
    @PostMapping("/program/{programId}")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @PathVariable programId: UUID,
        @Valid @RequestBody request: CreateProgramEditionRequest,
    ): ProgramEditionResponse =
        adminProgramEditionService.create(
            programId = programId,
            request = request,
        )

    @PreAuthorize("hasAuthority('programs:management:view')")
    @GetMapping("/program/{programId}")
    fun list(
        @PathVariable programId: UUID,

        @Min(value = 0, message = "La página no puede ser negativa.")
        @RequestParam(defaultValue = "0") page: Int,

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
    fun options(
        @PathVariable programId: UUID,
    ): List<ProgramEditionOptionResponse> =
        adminProgramEditionService.options(programId)

    @PreAuthorize("hasAuthority('programs:management:view')")
    @GetMapping("/{id}")
    fun findById(
        @PathVariable id: UUID,
    ): ProgramEditionResponse =
        adminProgramEditionService.get(id)

    @PreAuthorize("hasAuthority('programs:management:edit')")
    @PutMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateProgramEditionRequest,
    ): ProgramEditionResponse =
        adminProgramEditionService.update(
            id = id,
            request = request,
        )

    @PreAuthorize("hasAuthority('programs:management:edit')")
    @PatchMapping("/{id}/activate")
    fun activate(
        @PathVariable id: UUID,
    ): ProgramEditionResponse =
        adminProgramEditionService.activate(id)

    @PreAuthorize("hasAuthority('programs:management:edit')")
    @PatchMapping("/{id}/suspend")
    fun suspend(
        @PathVariable id: UUID,
    ): ProgramEditionResponse =
        adminProgramEditionService.suspend(id)

    @PreAuthorize("hasAuthority('programs:management:edit')")
    @PatchMapping("/{id}/close")
    fun close(
        @PathVariable id: UUID,
    ): ProgramEditionResponse =
        adminProgramEditionService.close(id)

    @PreAuthorize("hasAuthority('programs:management:edit')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @PathVariable id: UUID,
    ) {
        adminProgramEditionService.delete(id)
    }
}
