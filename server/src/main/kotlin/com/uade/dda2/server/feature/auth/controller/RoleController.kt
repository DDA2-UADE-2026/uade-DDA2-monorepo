package com.uade.dda2.server.feature.auth.controller

import com.uade.dda2.server.feature.auth.dto.request.CreateRoleRequest
import com.uade.dda2.server.feature.auth.dto.request.UpdateRoleRequest
import com.uade.dda2.server.feature.auth.dto.response.RoleResponse
import com.uade.dda2.server.feature.auth.service.RoleService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/roles")
@Tag(name = "Roles", description = "Administración de roles y sus permisos.")
class RoleController(
    private val roleService: RoleService,
) {
    @PreAuthorize("hasAuthority('roles:view')")
    @GetMapping
    @Operation(summary = "Listar roles", description = "Devuelve todos los roles con sus permisos asociados.")
    fun findAll(): List<RoleResponse> =
        roleService.findAll()

    @PreAuthorize("hasAuthority('roles:view')")
    @GetMapping("/{id}")
    @Operation(summary = "Consultar un rol", description = "Devuelve el rol identificado por su ID.")
    fun findById(@Parameter(description = "ID del rol.", example = "1") @PathVariable id: Long): RoleResponse =
        roleService.findById(id)

    @PreAuthorize("hasAuthority('roles:create')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear un rol", description = "Crea un rol y le asigna el conjunto de permisos indicado.")
    fun create(@Valid @RequestBody request: CreateRoleRequest): RoleResponse =
        roleService.create(request)

    @PreAuthorize("hasAuthority('roles:edit')")
    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un rol", description = "Reemplaza el nombre y los permisos del rol indicado.")
    fun update(
        @Parameter(description = "ID del rol.", example = "1") @PathVariable id: Long,
        @Valid @RequestBody request: UpdateRoleRequest,
    ): RoleResponse =
        roleService.update(id, request)

    @PreAuthorize("hasAuthority('roles:delete')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar un rol", description = "Elimina el rol indicado si no existen relaciones que lo impidan.")
    fun delete(@Parameter(description = "ID del rol.", example = "1") @PathVariable id: Long) {
        roleService.delete(id)
    }
}
