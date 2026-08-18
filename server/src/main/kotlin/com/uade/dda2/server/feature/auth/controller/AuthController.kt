package com.uade.dda2.server.feature.auth.controller

import com.uade.dda2.server.feature.auth.dto.request.LoginRequest
import com.uade.dda2.server.feature.auth.dto.response.LoginResponse
import com.uade.dda2.server.feature.auth.dto.response.MeResponse
import com.uade.dda2.server.feature.auth.service.AuthService
import com.uade.dda2.server.security.AuthCookieFactory
import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.web.csrf.CsrfToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService,
    private val authCookieFactory: AuthCookieFactory,
) {
    // Pisa la cookie XSRF-TOKEN (Spring la genera "lazy": si nada resuelve el
    // CsrfToken, nunca se manda). El front debe pegarle acá una vez al arrancar,
    // antes de cualquier POST/PUT/DELETE — incluido /auth/login.
    @GetMapping("/csrf")
    fun csrf(token: CsrfToken): ResponseEntity<Void> =
        ResponseEntity.noContent().build()

    // El login solo entrega cookie httpOnly: el JWT nunca viaja en el body.
    // Único modo de auth ahora es la cookie (Bearer quedó dropeado).
    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<LoginResponse> {
        val result = authService.login(request)
        val cookie = authCookieFactory.issue(result.token)

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .body(result.response)
    }

    @PostMapping("/logout")
    fun logout(): ResponseEntity<Void> {
        val cookie = authCookieFactory.clear()

        return ResponseEntity.status(HttpStatus.NO_CONTENT)
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .build()
    }

    @GetMapping("/me")
    fun me(): MeResponse =
        authService.me()
}
