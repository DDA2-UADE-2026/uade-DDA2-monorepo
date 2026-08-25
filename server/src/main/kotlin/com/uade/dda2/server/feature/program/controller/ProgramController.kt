package com.uade.dda2.server.feature.program.controller

import com.uade.dda2.server.feature.program.dto.request.CreateProgramRequest
import com.uade.dda2.server.feature.program.dto.request.UpdateProgramRequest
import com.uade.dda2.server.feature.program.dto.response.ProgramListResponse
import com.uade.dda2.server.feature.program.dto.response.ProgramResponse
import com.uade.dda2.server.feature.program.service.ProgramService
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/programs")
@Validated
class ProgramController(
    private val programService: ProgramService,
) {

    @PreAuthorize("hasAuthority('programs:create')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @Valid @RequestBody request: CreateProgramRequest,
    ): ProgramResponse =
        programService.create(request)

    @PreAuthorize("hasAuthority('programs:view')")
    @GetMapping
    fun list(
        @Min(value = 0, message = "La página no puede ser negativa.")
        @RequestParam(defaultValue = "0") page: Int,

        @Min(value = 1, message = "El tamaño de página debe ser mayor a cero.")
        @Max(value = 100, message = "El tamaño de página no puede superar 100 elementos.")
        @RequestParam(defaultValue = "20") size: Int,
    ): ProgramListResponse =
        programService.list(
            page = page,
            size = size,
        )

    @PreAuthorize("hasAuthority('programs:view')")
    @GetMapping("/{id}")
    fun findById(
        @PathVariable id: UUID,
    ): ProgramResponse =
        programService.get(id)

    @PreAuthorize("hasAuthority('programs:edit')")
    @PutMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateProgramRequest,
    ): ProgramResponse =
        programService.update(
            id = id,
            request = request,
        )

    @PreAuthorize("hasAuthority('programs:delete')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @PathVariable id: UUID,
    ) {
        programService.delete(id)
    }
}
