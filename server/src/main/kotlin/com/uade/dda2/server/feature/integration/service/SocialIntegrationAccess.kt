package com.uade.dda2.server.feature.integration.service

import com.uade.dda2.server.error.ForbiddenException
import com.uade.dda2.server.feature.application.validator.ApplicationValidator
import com.uade.dda2.server.feature.auth.entity.User
import com.uade.dda2.server.feature.auth.repository.UserRepository
import com.uade.dda2.server.feature.auth.service.CurrentUserService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SocialIntegrationAccess(
    private val users: UserRepository,
    private val currentUser: CurrentUserService,
    private val validator: ApplicationValidator,
) {
    @Transactional(readOnly = true)
    fun check(permission: String): User {
        val principal = currentUser.principal()
        val user = validator.validateUser(users.findByIdWithRoles(principal.id), principal, permission)
        if (user.roles.none { it.name == principal.activeRole && it.permissions.any { p -> p.name == permission } })
            throw ForbiddenException("AUTH_FORBIDDEN", "El rol activo no conserva el permiso requerido.")
        return user
    }
}
