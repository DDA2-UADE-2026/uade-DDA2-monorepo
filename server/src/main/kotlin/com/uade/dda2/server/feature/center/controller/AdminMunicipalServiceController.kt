package com.uade.dda2.server.feature.center.controller

import com.uade.dda2.server.feature.center.dto.request.CreateMunicipalServiceRequest
import com.uade.dda2.server.feature.center.dto.request.UpdateMunicipalServiceRequest
import com.uade.dda2.server.feature.center.dto.response.MunicipalServiceListResponse
import com.uade.dda2.server.feature.center.dto.response.MunicipalServiceResponse
import com.uade.dda2.server.feature.center.service.AdminMunicipalServiceService
import io.swagger.v3.oas.annotations.Operation
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
@RequestMapping("/api/admin/municipal-services", produces = ["application/json"])
@Validated
@Tag(name = "Servicios municipales", description = "Administración del catálogo municipal de servicios.")
class AdminMunicipalServiceController(
    private val service: AdminMunicipalServiceService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('services:management:create')")
    @Operation(operationId = "createMunicipalService", summary = "Crear un servicio municipal")
    fun create(@Valid @RequestBody request: CreateMunicipalServiceRequest): MunicipalServiceResponse =
        service.create(request)

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('services:management:view')")
    @Operation(operationId = "listMunicipalServices", summary = "Listar servicios municipales")
    fun list(
        @Min(0) @RequestParam(defaultValue = "0") page: Int,
        @Min(1) @Max(100) @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) active: Boolean?,
    ): MunicipalServiceListResponse = service.list(page, size, search, active)

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('services:management:view')")
    @Operation(operationId = "getMunicipalService", summary = "Consultar un servicio municipal")
    fun get(@PathVariable id: UUID): MunicipalServiceResponse = service.get(id)

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('services:management:edit')")
    @Operation(operationId = "updateMunicipalService", summary = "Editar un servicio municipal")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateMunicipalServiceRequest,
    ): MunicipalServiceResponse = service.update(id, request)

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('services:management:change-status')")
    @Operation(operationId = "activateMunicipalService", summary = "Activar un servicio municipal")
    fun activate(@PathVariable id: UUID): MunicipalServiceResponse = service.activate(id)

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('services:management:change-status')")
    @Operation(operationId = "deactivateMunicipalService", summary = "Desactivar un servicio municipal")
    fun deactivate(@PathVariable id: UUID): MunicipalServiceResponse = service.deactivate(id)
}
