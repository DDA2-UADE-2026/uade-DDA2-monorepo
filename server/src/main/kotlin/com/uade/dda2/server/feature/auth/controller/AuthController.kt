package com.uade.dda2.server.feature.auth.controller

import com.uade.dda2.server.feature.auth.dto.request.LoginRequest
import com.uade.dda2.server.feature.auth.dto.response.LoginResponse
import com.uade.dda2.server.feature.auth.dto.response.MeResponse
import com.uade.dda2.server.feature.auth.service.AuthService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService,
) {
    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): LoginResponse =
        authService.login(request)

    @GetMapping("/me")
    fun me(): MeResponse =
        authService.me()
}
