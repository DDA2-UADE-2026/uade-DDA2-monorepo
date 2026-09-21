package com.uade.dda2.server.feature.appointment.controller

import com.uade.dda2.server.error.ErrorResponse
import com.uade.dda2.server.feature.appointment.dto.request.CreateAppointmentRequest
import com.uade.dda2.server.feature.appointment.dto.response.AppointmentCenterResponse
import com.uade.dda2.server.feature.appointment.dto.response.AppointmentResponse
import com.uade.dda2.server.feature.appointment.dto.response.AppointmentServiceResponse
import com.uade.dda2.server.feature.appointment.dto.response.AvailableAppointmentSlotResponse
import com.uade.dda2.server.feature.appointment.service.AppointmentService
import com.uade.dda2.server.feature.appointment.service.AppointmentSlotService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.headers.Header
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/api/citizen", produces = ["application/json"])
@Tag(name = "Turnos ciudadanos", description = "Consulta de disponibilidad y otorgamiento de turnos propios.")
class CitizenAppointmentController(
    private val slotService: AppointmentSlotService,
    private val appointmentService: AppointmentService,
) {
    @GetMapping("/appointment-services")
    @PreAuthorize("hasRole('CIUDADANO') and hasAuthority('appointments:own:view')")
    @Operation(operationId = "listCitizenAppointmentServices", summary = "Listar servicios disponibles para turnos")
    fun listServices(): List<AppointmentServiceResponse> = slotService.listServices()

    @GetMapping("/appointment-services/{serviceId}/centers")
    @PreAuthorize("hasRole('CIUDADANO') and hasAuthority('appointments:own:view')")
    @Operation(operationId = "listCitizenAppointmentCenters", summary = "Listar centros disponibles para un servicio")
    fun listCenters(@PathVariable serviceId: UUID): List<AppointmentCenterResponse> =
        slotService.listCenters(serviceId)

    @GetMapping("/appointment-slots")
    @PreAuthorize("hasRole('CIUDADANO') and hasAuthority('appointments:own:view')")
    @Operation(operationId = "listCitizenAppointmentSlots", summary = "Listar horarios disponibles para una fecha")
    fun listSlots(
        @RequestParam centerServiceId: UUID,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
    ): List<AvailableAppointmentSlotResponse> = slotService.listSlots(centerServiceId, date)

    @PostMapping("/appointments")
    @PreAuthorize("hasRole('CIUDADANO') and hasAuthority('appointments:own:create')")
    @Operation(operationId = "createCitizenAppointment", summary = "Solicitar y confirmar un turno propio")
    @ApiResponse(responseCode = "201", description = "Turno confirmado.", headers = [
        Header(name = "Location", schema = Schema(type = "string")),
        Header(name = "Idempotency-Replayed", schema = Schema(type = "boolean", example = "false")),
    ])
    @ApiResponse(responseCode = "200", description = "Reintento idempotente del turno original.", headers = [
        Header(name = "Location", schema = Schema(type = "string")),
        Header(name = "Idempotency-Replayed", schema = Schema(type = "boolean", example = "true")),
    ])
    @ApiResponse(responseCode = "409", description = "Horario no disponible, superposicion o clave reutilizada.", content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))])
    fun create(
        @Valid @RequestBody request: CreateAppointmentRequest,
        @Parameter(description = "Obligatoria. 1-128 caracteres ASCII visibles sin espacios.")
        @RequestHeader(name = "Idempotency-Key") idempotencyKey: String,
    ): ResponseEntity<AppointmentResponse> {
        val result = appointmentService.create(request, idempotencyKey)
        return ResponseEntity.status(if (result.replayed) 200 else 201)
            .location(URI.create("/api/citizen/appointments/${result.appointment.id}"))
            .header("Idempotency-Replayed", result.replayed.toString())
            .body(result.appointment)
    }

    @GetMapping("/appointments/{appointmentId}")
    @PreAuthorize("hasRole('CIUDADANO') and hasAuthority('appointments:own:view')")
    @Operation(operationId = "getCitizenAppointment", summary = "Consultar un turno propio confirmado")
    @ApiResponse(responseCode = "404", description = "Turno inexistente o ajeno.", content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))])
    fun get(@PathVariable appointmentId: UUID): AppointmentResponse = appointmentService.get(appointmentId)
}
