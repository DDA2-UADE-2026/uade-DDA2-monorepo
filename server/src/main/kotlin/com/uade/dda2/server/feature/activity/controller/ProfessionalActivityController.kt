package com.uade.dda2.server.feature.activity.controller

import com.uade.dda2.server.feature.activity.dto.request.UpdateActivityAttendanceRequest
import com.uade.dda2.server.feature.activity.dto.response.ProfessionalActivityEnrollmentListResponse
import com.uade.dda2.server.feature.activity.dto.response.ProfessionalActivityEnrollmentResponse
import com.uade.dda2.server.feature.activity.dto.response.ProfessionalActivityListResponse
import com.uade.dda2.server.feature.activity.service.ProfessionalActivityService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/professional/activities", produces = ["application/json"])
@Validated
@Tag(name = "Asistencia a actividades", description = "Registro de asistencia por profesionales de centro.")
class ProfessionalActivityController(
    private val service: ProfessionalActivityService,
) {
    @GetMapping
    @PreAuthorize("hasAuthority('activities:attendance:view')")
    @Operation(summary = "Listar actividades habilitadas para registrar asistencia")
    fun listActivities(
        @Parameter(description = "Número de página, comenzando en cero.")
        @Min(0, message = "La página no puede ser negativa.")
        @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "Cantidad de elementos por página, entre 1 y 100.")
        @Min(1, message = "El tamaño de página debe ser mayor a cero.")
        @Max(100, message = "El tamaño de página no puede superar 100 elementos.")
        @RequestParam(defaultValue = "20") size: Int,
    ): ProfessionalActivityListResponse = service.listActivities(page, size)

    @GetMapping("/{activityId}/enrollments")
    @PreAuthorize("hasAuthority('activities:attendance:view')")
    @Operation(summary = "Listar personas inscriptas y su asistencia")
    fun listEnrollments(
        @PathVariable activityId: UUID,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
    ): ProfessionalActivityEnrollmentListResponse = service.listEnrollments(activityId, page, size)

    @PatchMapping("/{activityId}/enrollments/{enrollmentId}/attendance")
    @PreAuthorize("hasAuthority('activities:attendance:manage')")
    @Operation(summary = "Marcar a una persona como presente o ausente")
    fun updateAttendance(
        @PathVariable activityId: UUID,
        @PathVariable enrollmentId: UUID,
        @Valid @RequestBody request: UpdateActivityAttendanceRequest,
    ): ProfessionalActivityEnrollmentResponse =
        service.updateAttendance(activityId, enrollmentId, request)
}
