package com.uade.dda2.server.feature.program.controller

import com.uade.dda2.server.feature.program.dto.request.CreateProgramEditionRequest
import com.uade.dda2.server.feature.program.dto.request.UpdateProgramEditionRequest
import com.uade.dda2.server.feature.program.dto.response.ProgramEditionListResponse
import com.uade.dda2.server.feature.program.dto.response.ProgramEditionResponse
import com.uade.dda2.server.feature.program.service.ProgramEditionService
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/program-editions")
@Validated
class ProgramEditionController(
    private val programEditionService: ProgramEditionService,
) {

    @PreAuthorize("hasAuthority('programs:create')")
    @PostMapping("/program/{programId}")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @PathVariable programId: UUID,
        @Valid @RequestBody request: CreateProgramEditionRequest,
    ): ProgramEditionResponse =
        programEditionService.create(
            programId = programId,
            request = request,
        )

    @PreAuthorize("hasAuthority('programs:view')")
    @GetMapping("/program/{programId}")
    fun list(
        @PathVariable programId: UUID,

        @Min(value = 0, message = "La página no puede ser negativa.")
        @RequestParam(defaultValue = "0") page: Int,

        @Min(value = 1, message = "El tamaño de página debe ser mayor a cero.")
        @Max(value = 100, message = "El tamaño de página no puede superar 100 elementos.")
        @RequestParam(defaultValue = "20") size: Int,
    ): ProgramEditionListResponse =
        programEditionService.list(
            programId = programId,
            page = page,
            size = size,
        )

    @PreAuthorize("hasAuthority('programs:view')")
    @GetMapping("/{id}")
    fun findById(
        @PathVariable id: UUID,
    ): ProgramEditionResponse =
        programEditionService.get(id)

    @PreAuthorize("hasAuthority('programs:edit')")
    @PutMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateProgramEditionRequest,
    ): ProgramEditionResponse =
        programEditionService.update(
            id = id,
            request = request,
        )

    @PreAuthorize("hasAuthority('programs:edit')")
    @PatchMapping("/{id}/activate")
    fun activate(
        @PathVariable id: UUID,
    ): ProgramEditionResponse =
        programEditionService.activate(id)

    @PreAuthorize("hasAuthority('programs:edit')")
    @PatchMapping("/{id}/suspend")
    fun suspend(
        @PathVariable id: UUID,
    ): ProgramEditionResponse =
        programEditionService.suspend(id)

    @PreAuthorize("hasAuthority('programs:edit')")
    @PatchMapping("/{id}/close")
    fun close(
        @PathVariable id: UUID,
    ): ProgramEditionResponse =
        programEditionService.close(id)

    @PreAuthorize("hasAuthority('programs:delete')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @PathVariable id: UUID,
    ) {
        programEditionService.delete(id)
    }
}
