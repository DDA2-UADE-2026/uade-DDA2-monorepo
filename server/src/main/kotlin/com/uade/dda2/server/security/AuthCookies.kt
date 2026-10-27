package com.uade.dda2.server.security

import com.uade.dda2.server.config.AuthCookieProperties
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component

const val ACCESS_COOKIE = "uade_session"
const val ROLE_SELECTION_COOKIE = "uade_role_selection"
const val CSRF_COOKIE = "uade_csrf"

@Component
class AuthCookies(private val properties: AuthCookieProperties) {
    fun read(request: HttpServletRequest, name: String): String? =
        request.cookies?.firstOrNull { it.name == name }?.value?.takeIf { it.isNotBlank() }

    fun write(response: HttpServletResponse, name: String, value: String, lifetime: Long) {
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(name, value)
            .httpOnly(true)
            .secure(properties.secure)
            .sameSite(properties.sameSite)
            .path("/")
            .maxAge(lifetime)
            .build().toString())
    }

    fun clear(response: HttpServletResponse, name: String) = write(response, name, "", 0)
}

// An explicit Bearer header takes precedence, including an empty/invalid value.
// Never fall back to a cookie after the client explicitly chose Bearer.
fun HttpServletRequest.hasBearerAuthorization(): Boolean =
    getHeader(HttpHeaders.AUTHORIZATION)?.let {
        it.equals("Bearer", ignoreCase = true) || it.startsWith("Bearer ", ignoreCase = true)
    } == true
