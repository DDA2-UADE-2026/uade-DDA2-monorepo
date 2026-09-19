package com.uade.dda2.server.feature.center.controller

import com.uade.dda2.server.feature.center.dto.request.CreateCenterOpeningHourRequest
import com.uade.dda2.server.feature.center.dto.request.UpdateCenterOpeningHourRequest
import com.uade.dda2.server.feature.center.dto.response.CenterOpeningHourResponse
import com.uade.dda2.server.feature.center.service.AdminCenterOpeningHourService
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
@Tag(name = "Horarios de centros", description = "Administración de horarios semanales de apertura.")
class AdminCenterOpeningHourController(
    private val service: AdminCenterOpeningHourService,
) {
    @GetMapping("/municipal-centers/{centerId}/opening-hours")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('schedules:management:view')")
    @Operation(operationId = "listCenterOpeningHours", summary = "Listar horarios de apertura")
    fun list(@PathVariable centerId: UUID): List<CenterOpeningHourResponse> = service.list(centerId)

    @PostMapping("/municipal-centers/{centerId}/opening-hours")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('schedules:management:manage')")
    @Operation(operationId = "createCenterOpeningHour", summary = "Crear un horario de apertura")
    fun create(
        @PathVariable centerId: UUID,
        @Valid @RequestBody request: CreateCenterOpeningHourRequest,
    ): CenterOpeningHourResponse = service.create(centerId, request)

    @PutMapping("/center-opening-hours/{id}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('schedules:management:manage')")
    @Operation(operationId = "updateCenterOpeningHour", summary = "Editar un horario de apertura")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateCenterOpeningHourRequest,
    ): CenterOpeningHourResponse = service.update(id, request)

    @PatchMapping("/center-opening-hours/{id}/activate")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('schedules:management:manage')")
    @Operation(operationId = "activateCenterOpeningHour", summary = "Reactivar un horario de apertura")
    fun activate(@PathVariable id: UUID): CenterOpeningHourResponse = service.activate(id)

    @PatchMapping("/center-opening-hours/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('schedules:management:manage')")
    @Operation(operationId = "deactivateCenterOpeningHour", summary = "Desactivar un horario de apertura")
    fun deactivate(@PathVariable id: UUID): CenterOpeningHourResponse = service.deactivate(id)
}
