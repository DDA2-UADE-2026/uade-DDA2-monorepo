package com.uade.dda2.server.feature.auth.controller

import com.uade.dda2.server.error.UnauthorizedException
import com.uade.dda2.server.feature.auth.dto.request.LoginRequest
import com.uade.dda2.server.feature.auth.dto.request.SelectRoleRequest
import com.uade.dda2.server.feature.auth.dto.request.SwitchRoleRequest
import com.uade.dda2.server.feature.auth.dto.response.CookieSessionResponse
import com.uade.dda2.server.feature.auth.dto.response.LoginResponse
import com.uade.dda2.server.feature.auth.dto.response.SessionCsrfResponse
import com.uade.dda2.server.feature.auth.service.AuthService
import com.uade.dda2.server.security.ACCESS_COOKIE
import com.uade.dda2.server.security.AuthCookies
import com.uade.dda2.server.security.ROLE_SELECTION_COOKIE
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.web.csrf.CsrfToken
import org.springframework.security.web.csrf.CsrfTokenRepository
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/auth/session")
@Tag(name = "Sesión del navegador", description = "Autenticación con cookies HttpOnly y protección CSRF; no devuelve JWT en JSON.")
class CookieSessionController(
    private val authService: AuthService,
    private val cookies: AuthCookies,
    private val csrfTokens: CsrfTokenRepository,
) {
    @GetMapping("/csrf")
    @Operation(operationId = "sessionCsrf", summary = "Obtener token CSRF", description = "Consultar antes de operaciones de escritura. Enviar el token en X-XSRF-TOKEN y las cookies con credentials: include.")
    fun csrf(@Parameter(hidden = true) token: CsrfToken): SessionCsrfResponse = SessionCsrfResponse(token.headerName, token.token)

    @PostMapping("/login")
    @Operation(operationId = "sessionLogin", summary = "Iniciar sesión con cookie")
    fun login(@Valid @RequestBody body: LoginRequest, request: HttpServletRequest, response: HttpServletResponse): CookieSessionResponse =
        establish(authService.login(body), request, response)

    @GetMapping("/role-selection")
    @Operation(operationId = "sessionRoleSelection", summary = "Recuperar selección de rol pendiente", description = "Permite recargar la pantalla sin almacenar tokens en JavaScript. Responde 401 si no hay una selección vigente.")
    fun pending(request: HttpServletRequest): CookieSessionResponse =
        CookieSessionResponse(authService.pendingRoleSelection(selectionToken(request)), true, null)

    @PostMapping("/select-role")
    @Operation(operationId = "sessionSelectRole", summary = "Seleccionar el rol de la sesión")
    fun select(@Valid @RequestBody body: SwitchRoleRequest, request: HttpServletRequest, response: HttpServletResponse): CookieSessionResponse =
        establish(authService.selectRole(SelectRoleRequest(selectionToken(request), body.role)), request, response)

    @PostMapping("/switch-role")
    @Operation(operationId = "sessionSwitchRole", summary = "Cambiar el rol activo de la sesión")
    fun switch(@Valid @RequestBody body: SwitchRoleRequest, request: HttpServletRequest, response: HttpServletResponse): CookieSessionResponse =
        establish(authService.switchRole(body), request, response)

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "sessionLogout", summary = "Cerrar sesión del navegador", description = "Borra las cookies de acceso, selección y CSRF incluso si el JWT venció. No revoca tokens Bearer emitidos anteriormente.")
    fun logout(request: HttpServletRequest, response: HttpServletResponse) {
        cookies.clear(response, ACCESS_COOKIE)
        cookies.clear(response, ROLE_SELECTION_COOKIE)
        csrfTokens.saveToken(null, request, response)
    }

    private fun establish(result: LoginResponse, request: HttpServletRequest, response: HttpServletResponse): CookieSessionResponse {
        if (result.requiresRoleSelection) {
            cookies.clear(response, ACCESS_COOKIE)
            cookies.write(response, ROLE_SELECTION_COOKIE, requireNotNull(result.selectionToken), requireNotNull(result.selectionExpiresIn))
        } else {
            cookies.clear(response, ROLE_SELECTION_COOKIE)
            cookies.write(response, ACCESS_COOKIE, requireNotNull(result.token), requireNotNull(result.expiresIn))
        }
        csrfTokens.saveToken(null, request, response)
        return CookieSessionResponse(result.user, result.requiresRoleSelection, result.expiresIn ?: result.selectionExpiresIn)
    }

    private fun selectionToken(request: HttpServletRequest): String =
        cookies.read(request, ROLE_SELECTION_COOKIE)
            ?: throw UnauthorizedException("AUTH_INVALID_SELECTION_TOKEN", "La selección de rol venció. Iniciá sesión nuevamente.")
}
