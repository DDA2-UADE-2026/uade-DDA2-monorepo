package com.uade.dda2.server.feature.center.controller

import com.uade.dda2.server.feature.center.dto.request.AssignProfessionalRequest
import com.uade.dda2.server.feature.center.dto.response.ProfessionalAssignmentResponse
import com.uade.dda2.server.feature.center.service.AdminProfessionalAssignmentService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/admin", produces = ["application/json"])
@Tag(name = "Profesionales por servicio", description = "Asignación de profesionales a servicios de centros.")
class AdminProfessionalAssignmentController(
    private val service: AdminProfessionalAssignmentService,
) {
    @GetMapping("/center-services/{centerServiceId}/professionals")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('centers:management:view')")
    @Operation(operationId = "listProfessionalAssignments", summary = "Listar profesionales de un servicio del centro")
    fun list(@PathVariable centerServiceId: UUID): List<ProfessionalAssignmentResponse> = service.list(centerServiceId)

    @PostMapping("/center-services/{centerServiceId}/professionals")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('centers:management:edit')")
    @Operation(operationId = "assignProfessional", summary = "Asignar un profesional a un servicio del centro")
    fun assign(
        @PathVariable centerServiceId: UUID,
        @Valid @RequestBody request: AssignProfessionalRequest,
    ): ProfessionalAssignmentResponse = service.assign(centerServiceId, request)

    @PatchMapping("/professional-assignments/{id}/activate")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('centers:management:edit')")
    @Operation(operationId = "activateProfessionalAssignment", summary = "Reactivar una asignación profesional")
    fun activate(@PathVariable id: UUID): ProfessionalAssignmentResponse = service.activate(id)

    @PatchMapping("/professional-assignments/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('centers:management:edit')")
    @Operation(operationId = "deactivateProfessionalAssignment", summary = "Desactivar una asignación profesional")
    fun deactivate(@PathVariable id: UUID): ProfessionalAssignmentResponse = service.deactivate(id)
}
