package com.uade.dda2.server.feature.auth.controller

import com.uade.dda2.server.feature.auth.dto.request.CreateUserRequest
import com.uade.dda2.server.feature.auth.dto.request.UpdateUserRequest
import com.uade.dda2.server.feature.auth.dto.response.UserManagementResponse
import com.uade.dda2.server.feature.auth.service.UserManagementService
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
@RequestMapping("/users")
@Tag(name = "Usuarios", description = "Administración de usuarios, estado y roles asignados.")
class UserController(
    private val userManagementService: UserManagementService,
) {
    @PreAuthorize("hasAuthority('users:view')")
    @GetMapping
    @Operation(summary = "Listar usuarios", description = "Devuelve todos los usuarios con sus roles y permisos separados por rol.")
    fun findAll(): List<UserManagementResponse> =
        userManagementService.findAll()

    @PreAuthorize("hasAuthority('users:view')")
    @GetMapping("/{id}")
    @Operation(summary = "Consultar un usuario", description = "Devuelve el usuario identificado por su ID.")
    fun findById(@Parameter(description = "ID del usuario.", example = "1") @PathVariable id: Long): UserManagementResponse =
        userManagementService.findById(id)

    @PreAuthorize("hasAuthority('users:create')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear un usuario", description = "Registra un usuario y le asigna los roles indicados.")
    fun create(@Valid @RequestBody request: CreateUserRequest): UserManagementResponse =
        userManagementService.create(request)

    @PreAuthorize("hasAuthority('users:edit')")
    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un usuario", description = "Actualiza los datos, el estado y los roles del usuario indicado.")
    fun update(
        @Parameter(description = "ID del usuario.", example = "1") @PathVariable id: Long,
        @Valid @RequestBody request: UpdateUserRequest,
    ): UserManagementResponse =
        userManagementService.update(id, request)

    @PreAuthorize("hasAuthority('users:delete')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar un usuario", description = "Elimina el usuario indicado si no existen relaciones que lo impidan.")
    fun delete(@Parameter(description = "ID del usuario.", example = "1") @PathVariable id: Long) {
        userManagementService.delete(id)
    }
}
