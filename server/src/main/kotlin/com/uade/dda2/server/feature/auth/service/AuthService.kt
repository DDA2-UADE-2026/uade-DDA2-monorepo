package com.uade.dda2.server.feature.auth.service

import com.uade.dda2.server.config.JwtProperties
import com.uade.dda2.server.error.ForbiddenException
import com.uade.dda2.server.error.UnauthorizedException
import com.uade.dda2.server.feature.auth.dto.request.LoginRequest
import com.uade.dda2.server.feature.auth.dto.request.SelectRoleRequest
import com.uade.dda2.server.feature.auth.dto.request.SwitchRoleRequest
import com.uade.dda2.server.feature.auth.dto.response.LoginResponse
import com.uade.dda2.server.feature.auth.dto.response.MeResponse
import com.uade.dda2.server.feature.auth.entity.Role
import com.uade.dda2.server.feature.auth.entity.User
import com.uade.dda2.server.feature.auth.mapper.AuthMapper
import com.uade.dda2.server.feature.auth.repository.UserRepository
import com.uade.dda2.server.feature.auth.validator.AuthValidator
import com.uade.dda2.server.feature.log.service.LogService
import com.uade.dda2.server.security.JwtPrincipal
import com.uade.dda2.server.security.JwtService
import io.jsonwebtoken.JwtException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val authMapper: AuthMapper,
    private val authValidator: AuthValidator,
    private val jwtService: JwtService,
    private val jwtProperties: JwtProperties,
    private val logService: LogService,
) {
    @Transactional
    fun login(request: LoginRequest): LoginResponse {
        val user = authValidator.validLoginUser(
            user = userRepository.findByUsernameIgnoreCase(request.username.trim()),
            rawPassword = request.password,
        )
        ensureHasRoles(user)
        logService.recordLogin(user)

        if (user.roles.size == 1) {
            return activeRoleResponse(user, user.roles.single())
        }

        return LoginResponse(
            token = null,
            expiresIn = null,
            user = authMapper.toUserResponse(user, authMapper.roleNames(user), null, emptyList()),
            requiresRoleSelection = true,
            selectionToken = jwtService.createRoleSelectionToken(user),
            selectionExpiresIn = jwtProperties.roleSelectionExpirationSeconds,
        )
    }

    @Transactional(readOnly = true)
    fun selectRole(request: SelectRoleRequest): LoginResponse {
        val userId = try {
            jwtService.parseRoleSelectionToken(request.selectionToken)
        } catch (exception: JwtException) {
            throw invalidSelectionToken()
        } catch (exception: IllegalArgumentException) {
            throw invalidSelectionToken()
        }
        val user = activeUser(userId)
        return activeRoleResponse(user, assignedRole(user, request.role))
    }

    @Transactional(readOnly = true)
    fun switchRole(request: SwitchRoleRequest): LoginResponse {
        val user = activeUser(currentPrincipal().id)
        return activeRoleResponse(user, assignedRole(user, request.role))
    }

    @Transactional(readOnly = true)
    fun me(): MeResponse {
        val principal = currentPrincipal()
        val user = activeUser(principal.id)

        val roles = authMapper.roleNames(user)
        return MeResponse(
            user = authMapper.toUserResponse(
                user = user,
                roles = roles,
                activeRole = principal.activeRole,
                permissions = principal.permissions,
            ),
        )
    }

    private fun activeRoleResponse(user: User, role: Role): LoginResponse {
        val permissions = authMapper.permissionNames(role)
        return LoginResponse(
            token = jwtService.createToken(user, role),
            expiresIn = jwtProperties.expirationSeconds,
            user = authMapper.toUserResponse(user, authMapper.roleNames(user), role.name, permissions),
        )
    }

    private fun activeUser(id: Long): User =
        (userRepository.findByIdWithRoles(id) ?: throw authValidator.invalidCredentials())
            .also(authValidator::ensureActive)

    private fun ensureHasRoles(user: User) {
        if (user.roles.isEmpty()) {
            throw ForbiddenException("AUTH_NO_ROLES", "El usuario no tiene roles asignados.")
        }
    }

    private fun assignedRole(user: User, name: String): Role {
        ensureHasRoles(user)
        return user.roles.firstOrNull { it.name == name.trim() }
            ?: throw ForbiddenException("AUTH_ROLE_NOT_ASSIGNED", "El rol seleccionado no está asignado al usuario.")
    }

    private fun invalidSelectionToken() = UnauthorizedException(
        "AUTH_INVALID_SELECTION_TOKEN", "El token de selección es inválido o ha vencido.",
    )

    private fun currentPrincipal(): JwtPrincipal {
        val authentication = SecurityContextHolder.getContext().authentication
        return authentication?.principal as? JwtPrincipal
            ?: throw UnauthorizedException(
                code = "AUTH_UNAUTHENTICATED",
                message = "Unauthenticated.",
            )
    }
}
