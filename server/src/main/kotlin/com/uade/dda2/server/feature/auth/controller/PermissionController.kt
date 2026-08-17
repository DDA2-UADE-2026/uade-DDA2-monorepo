package com.uade.dda2.server.feature.auth.controller

import com.uade.dda2.server.feature.auth.dto.response.PermissionResponse
import com.uade.dda2.server.feature.auth.service.PermissionService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/permissions")
class PermissionController(
    private val permissionService: PermissionService,
) {
    @PreAuthorize("hasAuthority('permissions:view')")
    @GetMapping
    fun findAll(): List<PermissionResponse> =
        permissionService.findAll()
}
