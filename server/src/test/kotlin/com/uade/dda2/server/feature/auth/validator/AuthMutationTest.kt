package com.uade.dda2.server.feature.auth.validator

import com.uade.dda2.server.error.ApiException
import com.uade.dda2.server.feature.auth.entity.User
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.security.crypto.password.PasswordEncoder
import kotlin.test.*

class AuthMutationTest {
    @Test
    fun `login requires a local active account and matching encoded credentials`() {
        val encoder = mock(PasswordEncoder::class.java)
        val validator = AuthValidator(encoder)
        val valid = User(username = "qa", passwordHash = "encoded")
        `when`(encoder.matches("correct", "encoded")).thenReturn(true)
        assertSame(valid, validator.validLoginUser(valid, "correct"))
        verify(encoder).matches("correct", "encoded")
        val invalid = listOf(null, User(username = "qa", passwordHash = "encoded", active = false),
            User(username = null, passwordHash = "encoded"), User(username = " ", passwordHash = "encoded"),
            User(username = "qa", passwordHash = null), User(username = "qa", passwordHash = " "))
        invalid.forEach { user -> assertEquals("AUTH_INVALID_CREDENTIALS", assertFailsWith<ApiException> { validator.validLoginUser(user, "correct") }.code) }
        assertEquals("AUTH_INVALID_CREDENTIALS", assertFailsWith<ApiException> { validator.validLoginUser(valid, "wrong") }.code)
        validator.ensureActive(valid)
        valid.active = false
        assertEquals("AUTH_INVALID_CREDENTIALS", assertFailsWith<ApiException> { validator.ensureActive(valid) }.code)
    }
}
