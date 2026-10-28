package com.uade.dda2.server.config

import com.uade.dda2.server.feature.auth.entity.Role
import com.uade.dda2.server.feature.auth.entity.User
import com.uade.dda2.server.feature.auth.entity.Permission
import com.uade.dda2.server.feature.auth.repository.PermissionRepository
import com.uade.dda2.server.feature.auth.repository.RoleRepository
import com.uade.dda2.server.feature.auth.repository.UserRepository
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@ConditionalOnProperty(prefix = "app.bootstrap.admin", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class AdminUserInitializer(
    private val users: UserRepository,
    private val roles: RoleRepository,
    private val permissions: PermissionRepository,
    private val passwordEncoder: PasswordEncoder,
) : ApplicationRunner {
    @Transactional
    override fun run(args: ApplicationArguments) {
        val knownPermissions = permissions.findByNameIn(DEFAULT_PERMISSIONS).associateBy { it.name }
        val missingPermissions = DEFAULT_PERMISSIONS.minus(knownPermissions.keys)
            .map { Permission(name = it) }
        val permissionByName = knownPermissions + permissions.saveAll(missingPermissions).associateBy { it.name }

        val existingRoles = roles.findByNameIn(listOf("ADMIN", "CIUDADANO")).associateBy { it.name }
        val adminRole = existingRoles["ADMIN"] ?: roles.save(Role(name = "ADMIN"))
        val citizenRole = existingRoles["CIUDADANO"] ?: roles.save(Role(name = "CIUDADANO"))
        adminRole.permissions.addAll(permissionByName.values)
        citizenRole.permissions.addAll(CITIZEN_PERMISSIONS.map { requireNotNull(permissionByName[it]) })

        // Preserve credentials and status of existing accounts. An ADMIN account
        // also receives CIUDADANO so the role selector can issue a citizen JWT.
        if (!users.existsByUsernameIgnoreCase("admin")) {
            users.save(User(
                username = "admin",
                passwordHash = requireNotNull(passwordEncoder.encode("admin123")),
                name = "Administrator",
                email = "admin@example.com",
                roles = mutableSetOf(adminRole, citizenRole),
            ))
        }
        users.findByRolesId(requireNotNull(adminRole.id)).forEach { admin ->
            if (admin.roles.none { it.id == citizenRole.id }) admin.roles.add(citizenRole)
        }
    }

    companion object {
        // Keep in sync with the permission catalog in docs/database/init.sql.
        private val CITIZEN_PERMISSIONS = setOf(
            "activities:own:view", "activities:own:enroll",
            "applications:own:create", "applications:own:view",
            "applications:own:benefits:view", "applications:own:interventions:view",
            "applications:own:documents:view", "applications:own:documents:manage",
            "applications:own:visits:view", "applications:own:visits:reschedule-requests:create",
            "appointments:own:view", "appointments:own:create",
        )

        private val DEFAULT_PERMISSIONS = setOf(
            "permissions:view",
            "roles:view", "roles:create", "roles:edit", "roles:delete",
            "users:view", "users:create", "users:edit", "users:delete",
            "logs:view", "indicators:view",
            "activities:management:view", "activities:management:create",
            "activities:management:edit", "activities:management:change-status",
            "activities:own:view", "activities:own:enroll",
            "activities:attendance:view", "activities:attendance:manage",
            "programs:management:view", "programs:management:create", "programs:management:edit",
            "enrollment-periods:management:view", "enrollment-periods:management:create",
            "enrollment-periods:management:edit", "enrollment-periods:management:change-status",
            "applications:own:create", "applications:own:view",
            "applications:own:benefits:view", "applications:own:interventions:view",
            "applications:own:documents:view", "applications:own:documents:manage",
            "applications:own:visits:view", "applications:own:visits:reschedule-requests:create",
            "appointments:own:view", "appointments:own:create",
            "appointments:management:view", "appointments:management:manage",
            "appointments:professional:view", "appointments:professional:manage",
            "applications:management:create", "applications:management:view",
            "applications:management:resolve",
            "applications:management:benefits:assign", "applications:management:benefits:manage",
            "applications:evaluations:create", "applications:evaluations:view",
            "applications:interventions:create", "applications:interventions:view",
            "applications:interventions:follow-ups:create", "applications:interventions:follow-ups:publish",
            "applications:visits:create", "applications:visits:view", "applications:visits:manage",
            "applications:visits:results:create",
            "applications:visits:reschedule-requests:view", "applications:visits:reschedule-requests:review",
            "applications:management:documents:view", "applications:management:documents:review",
            "applications:management:documents:manage",
            "centers:management:view", "centers:management:create", "centers:management:edit",
            "centers:management:change-status",
            "services:management:view", "services:management:create", "services:management:edit",
            "services:management:change-status",
            "schedules:management:view", "schedules:management:manage",
        )
    }
}
