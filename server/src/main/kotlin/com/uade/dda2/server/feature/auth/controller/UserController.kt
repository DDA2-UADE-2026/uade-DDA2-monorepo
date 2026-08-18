package com.uade.dda2.server.feature.auth.controller

import com.uade.dda2.server.feature.auth.dto.request.CreateUserRequest
import com.uade.dda2.server.feature.auth.dto.request.UpdateUserRequest
import com.uade.dda2.server.feature.auth.dto.response.UserManagementResponse
import com.uade.dda2.server.feature.auth.service.UserManagementService
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
class UserController(
    private val userManagementService: UserManagementService,
) {
    @PreAuthorize("hasAuthority('users:view')")
    @GetMapping
    fun findAll(): List<UserManagementResponse> =
        userManagementService.findAll()

    @PreAuthorize("hasAuthority('users:view')")
    @GetMapping("/{id}")
    fun findById(@PathVariable id: Long): UserManagementResponse =
        userManagementService.findById(id)

    @PreAuthorize("hasAuthority('users:create')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: CreateUserRequest): UserManagementResponse =
        userManagementService.create(request)

    @PreAuthorize("hasAuthority('users:edit')")
    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateUserRequest,
    ): UserManagementResponse =
        userManagementService.update(id, request)

    @PreAuthorize("hasAuthority('users:delete')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) {
        userManagementService.delete(id)
    }
}
