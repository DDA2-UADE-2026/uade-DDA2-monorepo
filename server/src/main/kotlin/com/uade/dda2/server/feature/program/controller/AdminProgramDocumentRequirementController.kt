package com.uade.dda2.server.feature.program.controller

import com.uade.dda2.server.error.ErrorResponse
import com.uade.dda2.server.feature.program.dto.admin.request.CreateProgramDocumentRequirementRequest
import com.uade.dda2.server.feature.program.dto.admin.request.UpdateProgramDocumentRequirementRequest
import com.uade.dda2.server.feature.program.dto.admin.response.ProgramDocumentRequirementResponse
import com.uade.dda2.server.feature.program.service.AdminProgramDocumentRequirementService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/admin/program-editions/{editionId}/document-requirements")
@Tag(name = "Documentos requeridos por programas", description = "Catálogo documental de cada edición. Queda bloqueado al existir la primera solicitud.")
class AdminProgramDocumentRequirementController(private val service: AdminProgramDocumentRequirementService) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('programs:management:create')")
    @Operation(summary = "Crear un requisito documental")
    @ApiResponse(responseCode = "409", description = "Código duplicado, edición cerrada o catálogo bloqueado.", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    fun create(@PathVariable editionId: UUID, @Valid @RequestBody request: CreateProgramDocumentRequirementRequest) = service.create(editionId, request)

    @GetMapping
    @PreAuthorize("hasAuthority('programs:management:view')")
    @Operation(summary = "Listar requisitos documentales")
    fun list(@PathVariable editionId: UUID): List<ProgramDocumentRequirementResponse> = service.list(editionId)

    @GetMapping("/{requirementId}")
    @PreAuthorize("hasAuthority('programs:management:view')")
    @Operation(summary = "Consultar un requisito documental")
    fun get(@PathVariable editionId: UUID, @PathVariable requirementId: UUID) = service.get(editionId, requirementId)

    @PutMapping("/{requirementId}")
    @PreAuthorize("hasAuthority('programs:management:edit')")
    @Operation(summary = "Actualizar un requisito documental")
    @ApiResponse(responseCode = "409", description = "Código duplicado, edición cerrada o catálogo bloqueado.", content = [Content(schema = Schema(implementation = ErrorResponse::class))])
    fun update(@PathVariable editionId: UUID, @PathVariable requirementId: UUID, @Valid @RequestBody request: UpdateProgramDocumentRequirementRequest) =
        service.update(editionId, requirementId, request)

    @DeleteMapping("/{requirementId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('programs:management:edit')")
    @Operation(summary = "Eliminar un requisito documental")
    fun delete(@PathVariable editionId: UUID, @PathVariable requirementId: UUID) = service.delete(editionId, requirementId)
}
