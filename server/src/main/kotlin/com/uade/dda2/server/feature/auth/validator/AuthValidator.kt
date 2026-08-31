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
        val passwordHash = user?.passwordHash
        if (user == null || !user.active || user.username.isNullOrBlank() ||
            passwordHash.isNullOrBlank() || !passwordEncoder.matches(rawPassword, passwordHash)
        ) {
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
