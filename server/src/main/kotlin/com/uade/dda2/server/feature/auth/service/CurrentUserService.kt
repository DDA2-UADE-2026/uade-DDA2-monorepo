package com.uade.dda2.server.feature.auth.service

import com.uade.dda2.server.error.UnauthorizedException
import com.uade.dda2.server.feature.auth.entity.User
import com.uade.dda2.server.feature.auth.repository.UserRepository
import com.uade.dda2.server.security.JwtPrincipal
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
class CurrentUserService(
    private val userRepository: UserRepository,
) {
    fun principal(): JwtPrincipal {
        val authentication = SecurityContextHolder.getContext().authentication
        return authentication?.principal as? JwtPrincipal
            ?: throw UnauthorizedException(
                code = "AUTH_UNAUTHENTICATED",
                message = "Unauthenticated.",
            )
    }

    fun userReference(): User =
        userRepository.getReferenceById(principal().id)
}
