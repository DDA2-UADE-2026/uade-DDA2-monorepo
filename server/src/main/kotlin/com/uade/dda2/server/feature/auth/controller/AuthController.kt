package com.uade.dda2.server.feature.auth.controller

import com.uade.dda2.server.feature.auth.dto.request.LoginRequest
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
    @Operation(summary = "Iniciar sesión", description = "Valida las credenciales y devuelve un token JWT junto con los datos del usuario.")
    fun login(@Valid @RequestBody request: LoginRequest): LoginResponse =
        authService.login(request)

    @GetMapping("/me")
    @Operation(summary = "Consultar mi perfil", description = "Devuelve la identidad y los permisos del usuario autenticado.")
    fun me(): MeResponse =
        authService.me()
}
