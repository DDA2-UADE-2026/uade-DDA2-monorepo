package com.uade.dda2.server.feature.activity.controller

import com.uade.dda2.server.feature.activity.dto.response.ActivityEnrollmentResponse
import com.uade.dda2.server.feature.activity.dto.response.CitizenActivityListResponse
import com.uade.dda2.server.feature.activity.dto.response.CitizenActivityResponse
import com.uade.dda2.server.feature.activity.service.CitizenActivityService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.util.UUID

@RestController
@RequestMapping("/api/activities", produces = ["application/json"])
@Validated
@Tag(name = "Actividades ciudadanas", description = "Consulta e inscripción a actividades comunitarias.")
class CitizenActivityController(
    private val service: CitizenActivityService,
) {
    @GetMapping
    @PreAuthorize("hasAuthority('activities:own:view')")
    @Operation(summary = "Listar actividades abiertas")
    fun list(
        @Parameter(description = "Número de página, comenzando en cero.")
        @Min(0, message = "La página no puede ser negativa.")
        @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "Cantidad de elementos por página, entre 1 y 100.")
        @Min(1, message = "El tamaño de página debe ser mayor a cero.")
        @Max(100, message = "El tamaño de página no puede superar 100 elementos.")
        @RequestParam(defaultValue = "20") size: Int,
    ): CitizenActivityListResponse = service.list(page, size)

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('activities:own:view')")
    @Operation(summary = "Consultar una actividad abierta")
    fun get(@PathVariable id: UUID): CitizenActivityResponse = service.get(id)

    @PostMapping("/{id}/enrollments")
    @PreAuthorize("hasAuthority('activities:own:enroll')")
    @Operation(
        summary = "Inscribirse en una actividad",
        description = "La identidad ciudadana se obtiene del JWT. La actividad debe estar abierta y tener cupo.",
    )
    fun enroll(@PathVariable id: UUID): ResponseEntity<ActivityEnrollmentResponse> {
        val enrollment = service.enroll(id)
        return ResponseEntity.status(HttpStatus.CREATED)
            .location(URI.create("/api/activities/$id/enrollments/${enrollment.id}"))
            .body(enrollment)
    }
}
