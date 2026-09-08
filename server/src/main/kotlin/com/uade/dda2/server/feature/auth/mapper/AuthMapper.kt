package com.uade.dda2.server.feature.auth.mapper

import com.uade.dda2.server.feature.auth.dto.response.UserResponse
import com.uade.dda2.server.feature.auth.entity.Role
import com.uade.dda2.server.feature.auth.entity.User
import org.springframework.stereotype.Component

@Component
class AuthMapper {
    fun roleNames(user: User): List<String> =
        user.roles.map { it.name }.distinct().sorted()

    fun permissionNames(role: Role): List<String> =
        role.permissions
            .map { it.name }
            .distinct()
            .sorted()

    fun toUserResponse(
        user: User,
        roles: List<String>,
        activeRole: String?,
        permissions: List<String>,
    ): UserResponse =
        UserResponse(
            id = requireNotNull(user.id),
            username = user.username,
            name = user.name,
            email = user.email,
            roles = roles,
            activeRole = activeRole,
            permissions = permissions,
        )
}
