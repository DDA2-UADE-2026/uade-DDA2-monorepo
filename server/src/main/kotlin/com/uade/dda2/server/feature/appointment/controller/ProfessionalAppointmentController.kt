package com.uade.dda2.server.feature.appointment.controller

import com.uade.dda2.server.feature.appointment.dto.request.CorrectAttentionRequest
import com.uade.dda2.server.feature.appointment.dto.request.CreateAttentionRequest
import com.uade.dda2.server.feature.appointment.dto.response.CommunityAttentionResponse
import com.uade.dda2.server.feature.appointment.dto.response.ProfessionalAppointmentResponse
import com.uade.dda2.server.feature.appointment.service.CommunityAttentionService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/api/professional/appointments", produces = ["application/json"])
@Tag(name = "Atencion comunitaria", description = "Turnos propios y constancia de asistencia del profesional.")
class ProfessionalAppointmentController(private val service: CommunityAttentionService) {
    @GetMapping
    @PreAuthorize("hasRole('PROFESIONAL_CENTRO') and hasAuthority('appointments:professional:view')")
    @Operation(operationId = "listProfessionalAppointments", summary = "Listar turnos asignados para una fecha")
    fun list(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate): List<ProfessionalAppointmentResponse> =
        service.list(date)

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('PROFESIONAL_CENTRO') and hasAuthority('appointments:professional:view')")
    @Operation(operationId = "getProfessionalAppointment", summary = "Consultar un turno asignado y su constancia")
    fun get(@PathVariable id: UUID): ProfessionalAppointmentResponse = service.get(id)

    @PostMapping("/{id}/attention")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PROFESIONAL_CENTRO') and hasAuthority('appointments:professional:manage')")
    @Operation(operationId = "registerCommunityAttention", summary = "Registrar atendido o ausente")
    fun create(@PathVariable id: UUID, @RequestBody request: CreateAttentionRequest): CommunityAttentionResponse =
        service.create(id, request)

    @PutMapping("/{id}/attention")
    @PreAuthorize("hasRole('PROFESIONAL_CENTRO') and hasAuthority('appointments:professional:manage')")
    @Operation(operationId = "correctCommunityAttention", summary = "Corregir la constancia con su version vigente")
    fun correct(@PathVariable id: UUID, @RequestBody request: CorrectAttentionRequest): CommunityAttentionResponse =
        service.correct(id, request)
}
