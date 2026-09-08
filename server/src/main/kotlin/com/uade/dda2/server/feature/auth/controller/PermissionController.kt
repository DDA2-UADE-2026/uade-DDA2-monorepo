package com.uade.dda2.server.feature.auth.controller

import com.uade.dda2.server.feature.auth.dto.response.PermissionResponse
import com.uade.dda2.server.feature.auth.service.PermissionService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/permissions")
@Tag(name = "Permisos", description = "Consulta del catálogo de permisos disponibles.")
class PermissionController(
    private val permissionService: PermissionService,
) {
    @PreAuthorize("hasAuthority('permissions:view')")
    @GetMapping
    @Operation(summary = "Listar permisos", description = "Devuelve todos los permisos registrados en el sistema.")
    fun findAll(): List<PermissionResponse> =
        permissionService.findAll()
}
