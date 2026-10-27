package com.uade.dda2.server.feature.appointment.controller

import com.uade.dda2.server.feature.appointment.dto.response.AdminAppointmentResponse
import com.uade.dda2.server.feature.appointment.dto.request.RescheduleAppointmentRequest
import com.uade.dda2.server.feature.appointment.dto.response.AdminDaySummaryResponse
import com.uade.dda2.server.feature.appointment.dto.response.AdminFreeSlotResponse
import com.uade.dda2.server.feature.appointment.dto.response.AvailableAppointmentSlotResponse
import com.uade.dda2.server.feature.appointment.service.AdminAppointmentService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import java.time.LocalDate

@RestController
@RequestMapping("/api/admin/appointments", produces = ["application/json"])
@Tag(name = "Gestión de turnos", description = "Gestión de turnos de todos los centros municipales.")
class AdminAppointmentController(private val service: AdminAppointmentService) {
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('appointments:management:view')")
    @Operation(operationId = "listAdminAppointments", summary = "Buscar turnos por centro y fecha")
    fun list(
        @RequestParam centerId: UUID,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
    ): List<AdminAppointmentResponse> = service.list(centerId, date)

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('appointments:management:view')")
    @Operation(operationId = "getAdminAppointment", summary = "Consultar un turno para su gestión")
    fun get(@PathVariable id: UUID): AdminAppointmentResponse = service.get(id)

    @GetMapping("/monthly-summary")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('appointments:management:view')")
    @Operation(operationId = "listAdminAppointmentMonthlySummary", summary = "Resumir ocupación diaria de un centro en un mes")
    fun monthlySummary(
        @RequestParam centerId: UUID,
        @RequestParam @Min(2000) @Max(2100) year: Int,
        @RequestParam @Min(1) @Max(12) month: Int,
        @RequestParam(required = false) serviceId: UUID?,
    ): List<AdminDaySummaryResponse> = service.monthlySummary(centerId, year, month, serviceId)

    @GetMapping("/day-availability")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('appointments:management:view')")
    @Operation(operationId = "listAdminAppointmentDayAvailability", summary = "Listar horarios libres de un centro en una fecha")
    fun dayAvailability(
        @RequestParam centerId: UUID,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
        @RequestParam(required = false) serviceId: UUID?,
    ): List<AdminFreeSlotResponse> = service.dayAvailability(centerId, date, serviceId)

    @GetMapping("/{id}/slots")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('appointments:management:view')")
    @Operation(operationId = "listAdminAppointmentSlots", summary = "Consultar horarios para reprogramar un turno")
    fun listSlots(
        @PathVariable id: UUID,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
    ): List<AvailableAppointmentSlotResponse> = service.listSlots(id, date)

    @PutMapping("/{id}/schedule")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('appointments:management:manage')")
    @Operation(operationId = "rescheduleAdminAppointment", summary = "Reprogramar un turno pendiente")
    fun reschedule(@PathVariable id: UUID, @Valid @RequestBody request: RescheduleAppointmentRequest): AdminAppointmentResponse =
        service.reschedule(id, request)

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('appointments:management:manage')")
    @Operation(operationId = "cancelAdminAppointment", summary = "Cancelar un turno pendiente y retener su horario")
    fun cancel(@PathVariable id: UUID): AdminAppointmentResponse = service.cancel(id)

    @PatchMapping("/{id}/release-slot")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('appointments:management:manage')")
    @Operation(operationId = "releaseAdminAppointmentSlot", summary = "Habilitar el horario de un turno cancelado")
    fun release(@PathVariable id: UUID): AdminAppointmentResponse = service.release(id)
}
