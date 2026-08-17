package com.uade.dda2.server.feature.auth.controller

import com.uade.dda2.server.feature.auth.dto.request.CreateRoleRequest
import com.uade.dda2.server.feature.auth.dto.request.UpdateRoleRequest
import com.uade.dda2.server.feature.auth.dto.response.RoleResponse
import com.uade.dda2.server.feature.auth.service.RoleService
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
class RoleController(
    private val roleService: RoleService,
) {
    @PreAuthorize("hasAuthority('roles:view')")
    @GetMapping
    fun findAll(): List<RoleResponse> =
        roleService.findAll()

    @PreAuthorize("hasAuthority('roles:view')")
    @GetMapping("/{id}")
    fun findById(@PathVariable id: Long): RoleResponse =
        roleService.findById(id)

    @PreAuthorize("hasAuthority('roles:create')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: CreateRoleRequest): RoleResponse =
        roleService.create(request)

    @PreAuthorize("hasAuthority('roles:edit')")
    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateRoleRequest,
    ): RoleResponse =
        roleService.update(id, request)

    @PreAuthorize("hasAuthority('roles:delete')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) {
        roleService.delete(id)
    }
}
