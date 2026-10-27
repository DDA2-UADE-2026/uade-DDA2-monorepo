package com.uade.dda2.server.security

import io.jsonwebtoken.JwtException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtService: JwtService,
    private val securityErrorResponseWriter: SecurityErrorResponseWriter,
    private val cookies: AuthCookies,
) : OncePerRequestFilter() {
    // These routes must work when a previous access cookie is expired or malformed.
    override fun shouldNotFilter(request: HttpServletRequest): Boolean = request.requestURI.removePrefix(request.contextPath) in setOf(
        "/auth/session/csrf", "/auth/session/login", "/auth/session/select-role",
        "/auth/session/role-selection", "/auth/session/logout",
    )

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val token = if (request.hasBearerAuthorization()) {
            request.getHeader("Authorization").substringAfter(' ', "").trim()
        } else {
            cookies.read(request, ACCESS_COOKIE)
        }
        if (token == null) {
            filterChain.doFilter(request, response)
            return
        }

        val principal = try {
            jwtService.parse(token)
        } catch (exception: JwtException) {
            SecurityContextHolder.clearContext()
            securityErrorResponseWriter.write(
                request = request,
                response = response,
                status = HttpStatus.UNAUTHORIZED,
                code = "AUTH_INVALID_TOKEN",
                message = "Unauthenticated.",
            )
            return
        } catch (exception: IllegalArgumentException) {
            SecurityContextHolder.clearContext()
            securityErrorResponseWriter.write(
                request = request,
                response = response,
                status = HttpStatus.UNAUTHORIZED,
                code = "AUTH_INVALID_TOKEN",
                message = "Unauthenticated.",
            )
            return
        }

        val authorities = (principal.permissions + "ROLE_${principal.activeRole}")
            .distinct()
            .map(::SimpleGrantedAuthority)
        val authentication = UsernamePasswordAuthenticationToken(principal, null, authorities)
        authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
        SecurityContextHolder.getContext().authentication = authentication

        // Exceptions in controllers must not be misreported as malformed JWTs.
        filterChain.doFilter(request, response)
    }

}
