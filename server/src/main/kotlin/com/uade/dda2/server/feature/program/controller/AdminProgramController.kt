package com.uade.dda2.server.feature.program.controller

import com.uade.dda2.server.feature.program.dto.admin.request.CreateProgramRequest
import com.uade.dda2.server.feature.program.dto.admin.request.UpdateProgramRequest
import com.uade.dda2.server.feature.program.dto.admin.response.ProgramListResponse
import com.uade.dda2.server.feature.program.dto.admin.response.ProgramOptionResponse
import com.uade.dda2.server.feature.program.dto.admin.response.ProgramResponse
import com.uade.dda2.server.feature.program.service.AdminProgramService
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/admin/programs")
@Validated
class AdminProgramController(
    private val adminProgramService: AdminProgramService,
) {

    @PreAuthorize("hasAuthority('programs:management:create')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @Valid @RequestBody request: CreateProgramRequest,
    ): ProgramResponse =
        adminProgramService.create(request)

    @PreAuthorize("hasAuthority('programs:management:view')")
    @GetMapping
    fun list(
        @Min(value = 0, message = "La página no puede ser negativa.")
        @RequestParam(defaultValue = "0") page: Int,

        @Min(value = 1, message = "El tamaño de página debe ser mayor a cero.")
        @Max(value = 100, message = "El tamaño de página no puede superar 100 elementos.")
        @RequestParam(defaultValue = "20") size: Int,
    ): ProgramListResponse =
        adminProgramService.list(
            page = page,
            size = size,
        )

    @PreAuthorize("hasAuthority('programs:management:view')")
    @GetMapping("/options")
    fun options(): List<ProgramOptionResponse> =
        adminProgramService.options()

    @PreAuthorize("hasAuthority('programs:management:view')")
    @GetMapping("/{id}")
    fun findById(
        @PathVariable id: UUID,
    ): ProgramResponse =
        adminProgramService.get(id)

    @PreAuthorize("hasAuthority('programs:management:edit')")
    @PutMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateProgramRequest,
    ): ProgramResponse =
        adminProgramService.update(
            id = id,
            request = request,
        )

    @PreAuthorize("hasAuthority('programs:management:edit')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @PathVariable id: UUID,
    ) {
        adminProgramService.delete(id)
    }
}
