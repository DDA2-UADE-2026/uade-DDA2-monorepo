package com.uade.dda2.server.feature.auth.validator

import com.uade.dda2.server.error.UnauthorizedException
import com.uade.dda2.server.feature.auth.entity.User
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
class AuthValidator(
    private val passwordEncoder: PasswordEncoder,
) {
    fun validLoginUser(user: User?, rawPassword: String): User {
        if (user == null || !user.active || !passwordEncoder.matches(rawPassword, user.passwordHash)) {
            throw invalidCredentials()
        }

        return user
    }

    fun ensureActive(user: User) {
        if (!user.active) {
            throw invalidCredentials()
        }
    }

    fun invalidCredentials(): UnauthorizedException =
        UnauthorizedException(
            code = "AUTH_INVALID_CREDENTIALS",
            message = "Invalid username or password.",
        )
}
