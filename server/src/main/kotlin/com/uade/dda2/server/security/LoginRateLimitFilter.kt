package com.uade.dda2.server.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Small per-process guard for credential and role-selection endpoints.
 * Deployments with multiple application instances should additionally enforce
 * the same policy at their shared gateway.
 */
@Component
class LoginRateLimitFilter(
    private val errors: SecurityErrorResponseWriter,
    @Value("\${security.rate-limit.auth.max-attempts:10}") private val maxAttempts: Int,
    @Value("\${security.rate-limit.auth.window-millis:900000}") private val windowMillis: Long,
) : OncePerRequestFilter() {
    private val lock = Any()
    private val attemptsByAddress = mutableMapOf<String, ArrayDeque<Long>>()
    private val limitedPaths = setOf(
        "/auth/login",
        "/auth/select-role",
        "/auth/session/login",
        "/auth/session/m1/login",
        "/auth/session/select-role",
    )

    init {
        require(maxAttempts > 0) { "El límite de intentos debe ser positivo." }
        require(windowMillis > 0) { "La ventana de rate limiting debe ser positiva." }
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        request.method != "POST" || request.requestURI.removePrefix(request.contextPath) !in limitedPaths

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val address = request.remoteAddr ?: "unknown"
        val now = System.currentTimeMillis()
        val denied = synchronized(lock) {
            if (attemptsByAddress.size > 10_000) {
                val oldestAllowed = now - windowMillis
                attemptsByAddress.entries.removeIf { (_, attempts) ->
                    while (attempts.isNotEmpty() && attempts.first() <= oldestAllowed) attempts.removeFirst()
                    attempts.isEmpty()
                }
            }
            val attempts = attemptsByAddress.getOrPut(address) { ArrayDeque() }
            while (attempts.isNotEmpty() && now - attempts.first() >= windowMillis) attempts.removeFirst()
            if (attempts.size >= maxAttempts) {
                true
            } else {
                attempts.addLast(now)
                false
            }
        }

        if (denied) {
            response.setHeader("Retry-After", ((windowMillis + 999) / 1000).toString())
            errors.write(
                request = request,
                response = response,
                status = HttpStatus.TOO_MANY_REQUESTS,
                code = "AUTH_RATE_LIMITED",
                message = "Se alcanzó el límite de intentos. Esperá antes de volver a intentar.",
            )
            return
        }

        filterChain.doFilter(request, response)

        if (response.status in 200..299) {
            synchronized(lock) { attemptsByAddress.remove(address) }
        }
    }
}
