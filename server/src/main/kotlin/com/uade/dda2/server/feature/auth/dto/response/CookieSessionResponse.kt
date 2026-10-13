package com.uade.dda2.server.feature.auth.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Sesión del navegador. Los JWT se envían exclusivamente en cookies HttpOnly.")
data class CookieSessionResponse(
    val user: UserResponse,
    val requiresRoleSelection: Boolean,
    val expiresIn: Long?,
)

@Schema(description = "Token CSRF para enviar en el header indicado junto con las cookies del navegador.")
data class SessionCsrfResponse(val headerName: String, val token: String)
