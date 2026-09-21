package com.uade.dda2.server.feature.center.controller

import com.uade.dda2.server.feature.center.dto.request.CreateProfessionalAvailabilitiesRequest
import com.uade.dda2.server.feature.center.dto.request.CreateProfessionalAvailabilityRequest
import com.uade.dda2.server.feature.center.dto.request.UpdateProfessionalAvailabilityRequest
import com.uade.dda2.server.feature.center.dto.response.ProfessionalAvailabilityResponse
import com.uade.dda2.server.feature.center.service.AdminProfessionalAvailabilityService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/admin", produces = ["application/json"])
@Tag(name = "Disponibilidad profesional", description = "Administración de agendas semanales por servicio.")
class AdminProfessionalAvailabilityController(
    private val service: AdminProfessionalAvailabilityService,
) {
    @GetMapping("/professional-assignments/{assignmentId}/availability")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('schedules:management:view')")
    @Operation(operationId = "listProfessionalAvailability", summary = "Listar disponibilidad de una asignación")
    fun list(@PathVariable assignmentId: UUID): List<ProfessionalAvailabilityResponse> = service.list(assignmentId)

    @PostMapping("/professional-assignments/{assignmentId}/availability")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('schedules:management:manage')")
    @Operation(operationId = "createProfessionalAvailability", summary = "Crear una disponibilidad profesional")
    fun create(
        @PathVariable assignmentId: UUID,
        @Valid @RequestBody request: CreateProfessionalAvailabilityRequest,
    ): ProfessionalAvailabilityResponse = service.create(assignmentId, request)

    @PostMapping("/professional-assignments/{assignmentId}/availability/batch")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('schedules:management:manage')")
    @Operation(operationId = "createProfessionalAvailabilitiesBatch", summary = "Crear la misma disponibilidad en varios días")
    fun createBulk(
        @PathVariable assignmentId: UUID,
        @Valid @RequestBody request: CreateProfessionalAvailabilitiesRequest,
    ): List<ProfessionalAvailabilityResponse> = service.createBulk(assignmentId, request)

    @PutMapping("/professional-availability/{id}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('schedules:management:manage')")
    @Operation(operationId = "updateProfessionalAvailability", summary = "Editar una disponibilidad profesional")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateProfessionalAvailabilityRequest,
    ): ProfessionalAvailabilityResponse = service.update(id, request)

    @PatchMapping("/professional-availability/{id}/activate")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('schedules:management:manage')")
    @Operation(operationId = "activateProfessionalAvailability", summary = "Reactivar una disponibilidad profesional")
    fun activate(@PathVariable id: UUID): ProfessionalAvailabilityResponse = service.activate(id)

    @PatchMapping("/professional-availability/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('schedules:management:manage')")
    @Operation(operationId = "deactivateProfessionalAvailability", summary = "Desactivar una disponibilidad profesional")
    fun deactivate(@PathVariable id: UUID): ProfessionalAvailabilityResponse = service.deactivate(id)
}
