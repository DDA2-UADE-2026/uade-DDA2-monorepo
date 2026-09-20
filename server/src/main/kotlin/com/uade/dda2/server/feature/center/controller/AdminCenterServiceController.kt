package com.uade.dda2.server.feature.center.controller

import com.uade.dda2.server.feature.center.dto.request.AssignCenterServiceRequest
import com.uade.dda2.server.feature.center.dto.response.CenterServiceResponse
import com.uade.dda2.server.feature.center.service.AdminCenterServiceService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/admin", produces = ["application/json"])
@Tag(name = "Servicios por centro", description = "Asignación del catálogo de servicios a centros municipales.")
class AdminCenterServiceController(
    private val service: AdminCenterServiceService,
) {
    @GetMapping("/municipal-centers/{centerId}/services")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('centers:management:view')")
    @Operation(operationId = "listCenterServices", summary = "Listar servicios de un centro")
    fun list(@PathVariable centerId: UUID): List<CenterServiceResponse> = service.list(centerId)

    @PostMapping("/municipal-centers/{centerId}/services")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('centers:management:edit')")
    @Operation(operationId = "assignServiceToCenter", summary = "Asignar un servicio a un centro")
    fun assign(
        @PathVariable centerId: UUID,
        @Valid @RequestBody request: AssignCenterServiceRequest,
    ): CenterServiceResponse = service.assign(centerId, request)

    @PatchMapping("/center-services/{id}/activate")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('centers:management:edit')")
    @Operation(operationId = "activateCenterService", summary = "Reactivar un servicio del centro")
    fun activate(@PathVariable id: UUID): CenterServiceResponse = service.activate(id)

    @PatchMapping("/center-services/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('centers:management:edit')")
    @Operation(operationId = "deactivateCenterService", summary = "Desactivar un servicio del centro")
    fun deactivate(@PathVariable id: UUID): CenterServiceResponse = service.deactivate(id)
}
