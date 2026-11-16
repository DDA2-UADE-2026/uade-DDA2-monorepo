package com.uade.dda2.server.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import tools.jackson.databind.json.JsonMapper
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LoginRateLimitFilterTest {
    private val errors = SecurityErrorResponseWriter(JsonMapper.builder().build())

    private fun login(address: String = "10.0.0.1", path: String = "/auth/login") =
        MockHttpServletRequest("POST", path).apply { remoteAddr = address }

    private fun chainReturning(status: Int) = FilterChain { _, response -> (response as HttpServletResponse).status = status }

    private fun MockHttpServletRequest.through(filter: LoginRateLimitFilter, status: Int = 401): MockHttpServletResponse =
        MockHttpServletResponse().also { filter.doFilter(this, it, chainReturning(status)) }

    @Suppress("UNCHECKED_CAST")
    private fun trackedAddresses(filter: LoginRateLimitFilter): Map<String, ArrayDeque<Long>> =
        LoginRateLimitFilter::class.java.getDeclaredField("attemptsByAddress")
            .apply { isAccessible = true }
            .get(filter) as Map<String, ArrayDeque<Long>>

    @Test
    fun `bloquea la direccion que supera el limite y avisa cuando reintentar`() {
        val filter = LoginRateLimitFilter(errors, maxAttempts = 2, windowMillis = 60_000)

        assertEquals(401, login().through(filter).status)
        assertEquals(401, login().through(filter).status)
        val limited = login().through(filter)

        assertEquals(429, limited.status)
        assertEquals("60", limited.getHeader("Retry-After"))
        assertTrue(limited.contentAsString.contains("AUTH_RATE_LIMITED"))
        assertEquals(401, login(address = "10.0.0.2").through(filter).status)
    }

    @Test
    fun `un login exitoso limpia los intentos de la direccion`() {
        val filter = LoginRateLimitFilter(errors, maxAttempts = 2, windowMillis = 60_000)

        login().through(filter)
        login().through(filter, status = 200)

        assertFalse("10.0.0.1" in trackedAddresses(filter))
        assertEquals(401, login().through(filter).status)
    }

    @Test
    fun `solo limita los POST a los endpoints de credenciales`() {
        val filter = LoginRateLimitFilter(errors, maxAttempts = 1, windowMillis = 60_000)

        repeat(3) { assertEquals(401, MockHttpServletRequest("GET", "/auth/login").through(filter).status) }
        repeat(3) { assertEquals(401, login(path = "/api/programs").through(filter).status) }
        assertTrue(trackedAddresses(filter).isEmpty())
    }

    @Test
    fun `descarta las direcciones vencidas cuando el registro crece demasiado`() {
        val filter = LoginRateLimitFilter(errors, maxAttempts = 5, windowMillis = 1)

        repeat(10_001) { index -> login(address = "10.1.${index / 256}.${index % 256}").through(filter) }
        assertEquals(10_001, trackedAddresses(filter).size)

        Thread.sleep(5)
        login(address = "192.168.0.1").through(filter)

        assertEquals(setOf("192.168.0.1"), trackedAddresses(filter).keys)
    }
}
