package com.uade.dda2.server.feature.center.controller

import com.uade.dda2.server.feature.center.dto.request.CreateMunicipalCenterRequest
import com.uade.dda2.server.feature.center.dto.request.UpdateMunicipalCenterRequest
import com.uade.dda2.server.feature.center.dto.response.MunicipalCenterListResponse
import com.uade.dda2.server.feature.center.dto.response.MunicipalCenterResponse
import com.uade.dda2.server.feature.center.service.AdminMunicipalCenterService
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
@RequestMapping("/api/admin/municipal-centers", produces = ["application/json"])
@Validated
@Tag(name = "Centros municipales", description = "Administración de centros municipales.")
class AdminMunicipalCenterController(
    private val service: AdminMunicipalCenterService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('centers:management:create')")
    @Operation(operationId = "createMunicipalCenter", summary = "Crear un centro municipal")
    fun create(@Valid @RequestBody request: CreateMunicipalCenterRequest): MunicipalCenterResponse =
        service.create(request)

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('centers:management:view')")
    @Operation(operationId = "listMunicipalCenters", summary = "Listar centros municipales")
    fun list(
        @Min(0) @RequestParam(defaultValue = "0") page: Int,
        @Min(1) @Max(100) @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) active: Boolean?,
    ): MunicipalCenterListResponse = service.list(page, size, search, active)

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('centers:management:view')")
    @Operation(operationId = "getMunicipalCenter", summary = "Consultar un centro municipal")
    fun get(@PathVariable id: UUID): MunicipalCenterResponse = service.get(id)

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('centers:management:edit')")
    @Operation(operationId = "updateMunicipalCenter", summary = "Editar un centro municipal")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateMunicipalCenterRequest,
    ): MunicipalCenterResponse = service.update(id, request)

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('centers:management:change-status')")
    @Operation(operationId = "activateMunicipalCenter", summary = "Activar un centro municipal")
    fun activate(@PathVariable id: UUID): MunicipalCenterResponse = service.activate(id)

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('centers:management:change-status')")
    @Operation(operationId = "deactivateMunicipalCenter", summary = "Desactivar un centro municipal")
    fun deactivate(@PathVariable id: UUID): MunicipalCenterResponse = service.deactivate(id)
}
