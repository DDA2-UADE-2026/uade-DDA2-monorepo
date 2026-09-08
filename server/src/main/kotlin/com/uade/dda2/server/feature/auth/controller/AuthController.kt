package com.uade.dda2.server.feature.auth.controller

import com.uade.dda2.server.feature.auth.dto.request.LoginRequest
import com.uade.dda2.server.feature.auth.dto.request.SelectRoleRequest
import com.uade.dda2.server.feature.auth.dto.request.SwitchRoleRequest
import com.uade.dda2.server.feature.auth.dto.response.LoginResponse
import com.uade.dda2.server.feature.auth.dto.response.MeResponse
import com.uade.dda2.server.feature.auth.service.AuthService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticación", description = "Inicio de sesión y consulta de la identidad autenticada.")
class AuthController(
    private val authService: AuthService,
) {
    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión", description = "Un rol: emite un JWT operativo. Varios roles: devuelve selectionToken sin permisos operativos; continuar con /auth/select-role. Sin roles: 403.")
    fun login(@Valid @RequestBody request: LoginRequest): LoginResponse =
        authService.login(request)

    @PostMapping("/select-role")
    @Operation(summary = "Seleccionar rol después del login", description = "Valida el JWT temporal del body y la asignación actual del rol. Emite un JWT operativo con solo los permisos de ese rol. No enviar bearer.")
    fun selectRole(@Valid @RequestBody request: SelectRoleRequest): LoginResponse =
        authService.selectRole(request)

    @PostMapping("/switch-role")
    @Operation(summary = "Cambiar rol activo", description = "Requiere JWT operativo y valida el usuario activo y el rol actual en la base. Emite un nuevo JWT; el anterior sigue válido hasta vencer.")
    fun switchRole(@Valid @RequestBody request: SwitchRoleRequest): LoginResponse =
        authService.switchRole(request)

    @GetMapping("/me")
    @Operation(summary = "Consultar mi perfil", description = "Devuelve datos y roles asignados actuales; el rol activo y sus permisos corresponden al JWT utilizado, sin combinar otros roles.")
    fun me(): MeResponse =
        authService.me()
}
