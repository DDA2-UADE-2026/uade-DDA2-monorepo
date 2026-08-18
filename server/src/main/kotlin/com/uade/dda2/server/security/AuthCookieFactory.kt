package com.uade.dda2.server.security

import com.uade.dda2.server.config.JwtProperties
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component

@Component
class AuthCookieFactory(
    private val jwtProperties: JwtProperties,
) {
    // Max-Age igual a la expiración del JWT: el browser borra la cookie sola
    // cuando el token vence. La validación real sigue siendo el claim "exp"
    // en JwtService.parse(), esto es solo prolijidad del lado del cliente.
    fun issue(token: String): ResponseCookie =
        baseCookie(token)
            .maxAge(jwtProperties.expirationSeconds)
            .build()

    fun clear(): ResponseCookie =
        baseCookie("")
            .maxAge(0)
            .build()

    private fun baseCookie(value: String): ResponseCookie.ResponseCookieBuilder =
        ResponseCookie.from(jwtProperties.cookieName, value)
            .httpOnly(true)
            .secure(jwtProperties.cookieSecure)
            .sameSite("Lax")
            .path("/")
}
