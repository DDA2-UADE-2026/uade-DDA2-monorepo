package com.uade.dda2.server.feature.integration.service

import com.uade.dda2.server.error.ForbiddenException
import com.uade.dda2.server.feature.application.validator.ApplicationValidator
import com.uade.dda2.server.feature.auth.repository.UserRepository
import com.uade.dda2.server.feature.auth.service.CurrentUserService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CoreDashboardAccess(private val users: UserRepository, private val currentUser: CurrentUserService,
    private val validator: ApplicationValidator) {
    companion object {
        const val CORE_VIEW_PERMISSION = "integrations:core:view"
    }

    // Termina la transacción de autorización antes de iniciar cualquier consulta de red.
    @Transactional(readOnly = true)
    fun check() {
        val principal = currentUser.principal()
        val user = validator.validateUser(users.findByIdWithRoles(principal.id), principal, CORE_VIEW_PERMISSION)
        if (user.roles.none { it.name == principal.activeRole && it.permissions.any { permission -> permission.name == CORE_VIEW_PERMISSION } })
            throw ForbiddenException("AUTH_FORBIDDEN", "El rol activo no está autorizado para consultar Core.")
    }
}
