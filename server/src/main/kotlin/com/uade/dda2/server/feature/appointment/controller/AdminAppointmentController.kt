package com.uade.dda2.server.feature.appointment.controller

import com.uade.dda2.server.feature.appointment.dto.response.AppointmentResponse
import com.uade.dda2.server.feature.appointment.service.AdminAppointmentService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/admin/appointments", produces = ["application/json"])
@Tag(name = "Gestión de turnos", description = "Gestión de turnos de todos los centros municipales.")
class AdminAppointmentController(private val service: AdminAppointmentService) {
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('appointments:management:view')")
    @Operation(operationId = "getAdminAppointment", summary = "Consultar un turno para su gestión")
    fun get(@PathVariable id: UUID): AppointmentResponse = service.get(id)

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('appointments:management:manage')")
    @Operation(operationId = "cancelAdminAppointment", summary = "Cancelar un turno pendiente y retener su horario")
    fun cancel(@PathVariable id: UUID): AppointmentResponse = service.cancel(id)

    @PatchMapping("/{id}/release-slot")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('appointments:management:manage')")
    @Operation(operationId = "releaseAdminAppointmentSlot", summary = "Habilitar el horario de un turno cancelado")
    fun release(@PathVariable id: UUID): AppointmentResponse = service.release(id)
}
