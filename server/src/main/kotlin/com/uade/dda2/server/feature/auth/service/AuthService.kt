package com.uade.dda2.server.feature.auth.service

import com.uade.dda2.server.config.JwtProperties
import com.uade.dda2.server.error.UnauthorizedException
import com.uade.dda2.server.feature.auth.dto.request.LoginRequest
import com.uade.dda2.server.feature.auth.dto.response.LoginResponse
import com.uade.dda2.server.feature.auth.dto.response.MeResponse
import com.uade.dda2.server.feature.auth.mapper.AuthMapper
import com.uade.dda2.server.feature.auth.repository.UserRepository
import com.uade.dda2.server.feature.auth.validator.AuthValidator
import com.uade.dda2.server.feature.log.service.LogService
import com.uade.dda2.server.security.JwtPrincipal
import com.uade.dda2.server.security.JwtService
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
        val roles = authMapper.roleNames(user)
        val permissions = authMapper.permissionNames(user)
        val token = jwtService.createToken(user, roles, permissions)
        val userResponse = authMapper.toUserResponse(
            user = user,
            roles = roles,
            permissions = permissions,
        )
        logService.recordLogin(user)

        return LoginResponse(
            token = token,
            expiresIn = jwtProperties.expirationSeconds,
            user = userResponse,
            permissions = permissions,
        )
    }

    @Transactional(readOnly = true)
    fun me(): MeResponse {
        val principal = currentPrincipal()
        val user = userRepository.findByIdWithRoles(principal.id) ?: throw authValidator.invalidCredentials()

        authValidator.ensureActive(user)

        val roles = authMapper.roleNames(user)
        val permissions = authMapper.permissionNames(user)
        return MeResponse(
            user = authMapper.toUserResponse(
                user = user,
                roles = roles,
                permissions = permissions,
            )
        )
    }

    private fun currentPrincipal(): JwtPrincipal {
        val authentication = SecurityContextHolder.getContext().authentication
        return authentication?.principal as? JwtPrincipal
            ?: throw UnauthorizedException(
                code = "AUTH_UNAUTHENTICATED",
                message = "Unauthenticated.",
            )
    }
}
