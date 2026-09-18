package com.uade.dda2.server.feature.activity.controller

import com.uade.dda2.server.feature.activity.dto.request.CreateActivityRequest
import com.uade.dda2.server.feature.activity.dto.request.UpdateActivityRequest
import com.uade.dda2.server.feature.activity.dto.response.ActivityListResponse
import com.uade.dda2.server.feature.activity.dto.response.ActivityResponse
import com.uade.dda2.server.feature.activity.service.AdminActivityService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/admin/activities", produces = ["application/json"])
@Validated
@Tag(name = "Actividades", description = "Administración de actividades comunitarias.")
class AdminActivityController(
    private val adminActivityService: AdminActivityService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('activities:management:create')")
    @Operation(summary = "Crear una actividad", description = "Registra una actividad comunitaria en estado DRAFT.")
    fun create(@Valid @RequestBody request: CreateActivityRequest): ActivityResponse =
        adminActivityService.create(request)

    @GetMapping
    @PreAuthorize("hasAuthority('activities:management:view')")
    @Operation(summary = "Listar actividades", description = "Devuelve una página de actividades comunitarias.")
    fun list(
        @Parameter(description = "Número de página, comenzando en cero.")
        @Min(0, message = "La página no puede ser negativa.")
        @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "Cantidad de elementos por página, entre 1 y 100.")
        @Min(1, message = "El tamaño de página debe ser mayor a cero.")
        @Max(100, message = "El tamaño de página no puede superar 100 elementos.")
        @RequestParam(defaultValue = "20") size: Int,
    ): ActivityListResponse = adminActivityService.list(page, size)

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('activities:management:view')")
    @Operation(summary = "Consultar una actividad")
    fun get(@PathVariable id: UUID): ActivityResponse = adminActivityService.get(id)

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('activities:management:edit')")
    @Operation(summary = "Actualizar una actividad", description = "Solo se pueden modificar actividades en estado DRAFT.")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateActivityRequest,
    ): ActivityResponse = adminActivityService.update(id, request)

    @PatchMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('activities:management:change-status')")
    @Operation(summary = "Publicar una actividad", description = "Cambia una actividad de DRAFT a OPEN.")
    fun publish(@PathVariable id: UUID): ActivityResponse = adminActivityService.publish(id)

    @PatchMapping("/{id}/close")
    @PreAuthorize("hasAuthority('activities:management:change-status')")
    @Operation(summary = "Cerrar una actividad", description = "Cambia una actividad de OPEN a CLOSED.")
    fun close(@PathVariable id: UUID): ActivityResponse = adminActivityService.close(id)
}
