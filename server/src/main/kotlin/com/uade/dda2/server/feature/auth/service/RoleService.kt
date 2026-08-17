package com.uade.dda2.server.feature.auth.service

import com.uade.dda2.server.error.BadRequestException
import com.uade.dda2.server.error.ConflictException
import com.uade.dda2.server.error.NotFoundException
import com.uade.dda2.server.feature.auth.dto.request.CreateRoleRequest
import com.uade.dda2.server.feature.auth.dto.request.UpdateRoleRequest
import com.uade.dda2.server.feature.auth.dto.response.RoleResponse
import com.uade.dda2.server.feature.auth.entity.Permission
import com.uade.dda2.server.feature.auth.entity.Role
import com.uade.dda2.server.feature.auth.repository.PermissionRepository
import com.uade.dda2.server.feature.auth.repository.RoleRepository
import com.uade.dda2.server.feature.auth.repository.UserRepository
import com.uade.dda2.server.feature.log.entity.LogAction
import com.uade.dda2.server.feature.log.entity.LogEntityType
import com.uade.dda2.server.feature.log.service.LogService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.json.JsonMapper

@Service
class RoleService(
    private val roleRepository: RoleRepository,
    private val permissionRepository: PermissionRepository,
    private val userRepository: UserRepository,
    private val currentUserService: CurrentUserService,
    private val logService: LogService,
    private val jsonMapper: JsonMapper,
) {
    @Transactional(readOnly = true)
    fun findAll(): List<RoleResponse> =
        roleRepository.findAllByOrderByNameAsc().map(::toResponse)

    @Transactional(readOnly = true)
    fun findById(id: Long): RoleResponse =
        toResponse(findRole(id))

    @Transactional
    fun create(request: CreateRoleRequest): RoleResponse {
        val name = normalizeRoleName(request.name)
        if (roleRepository.existsByNameIgnoreCase(name)) {
            throw roleNameConflict(name)
        }

        val role = roleRepository.save(
            Role(
                name = name,
                permissions = resolvePermissions(request.permissions).toMutableSet(),
            ),
        )
        val response = toResponse(role)
        logService.record(
            user = currentUserService.userReference(),
            action = LogAction.CREATE,
            entityType = LogEntityType.ROLE,
            entityId = requireNotNull(role.id).toString(),
            newValues = json(roleSnapshot(role)),
        )

        return response
    }

    @Transactional
    fun update(id: Long, request: UpdateRoleRequest): RoleResponse {
        val role = findRole(id)
        val name = normalizeRoleName(request.name)
        if (roleRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw roleNameConflict(name)
        }

        val oldValues = json(roleSnapshot(role))
        role.name = name
        role.permissions.clear()
        role.permissions.addAll(resolvePermissions(request.permissions))

        val response = toResponse(role)
        logService.record(
            user = currentUserService.userReference(),
            action = LogAction.UPDATE,
            entityType = LogEntityType.ROLE,
            entityId = id.toString(),
            oldValues = oldValues,
            newValues = json(roleSnapshot(role)),
        )

        return response
    }

    @Transactional
    fun delete(id: Long) {
        val role = findRole(id)
        val oldValues = json(roleSnapshot(role))

        userRepository.findByRolesId(id).forEach { user ->
            user.roles.removeIf { userRole -> userRole.id == id }
        }
        role.permissions.clear()
        roleRepository.delete(role)

        logService.record(
            user = currentUserService.userReference(),
            action = LogAction.DELETE,
            entityType = LogEntityType.ROLE,
            entityId = id.toString(),
            oldValues = oldValues,
        )
    }

    private fun findRole(id: Long): Role =
        roleRepository.findByIdWithPermissions(id)
            ?: throw NotFoundException(
                code = "ROLE_NOT_FOUND",
                message = "Role not found.",
            )

    private fun resolvePermissions(permissionNames: List<String>): Set<Permission> {
        val names = permissionNames.map(String::trim).filter(String::isNotBlank).distinct().toSet()
        if (names.size != permissionNames.size) {
            throw BadRequestException(
                code = "ROLE_INVALID_PERMISSIONS",
                message = "Permissions must not contain blank or duplicated values.",
            )
        }
        if (names.isEmpty()) {
            return emptySet()
        }

        val permissions = permissionRepository.findByNameIn(names).associateBy { it.name }
        val missing = names.filterNot(permissions::containsKey)
        if (missing.isNotEmpty()) {
            throw BadRequestException(
                code = "ROLE_UNKNOWN_PERMISSIONS",
                message = "Unknown permissions: ${missing.sorted().joinToString(", ")}.",
            )
        }

        return names.mapNotNull(permissions::get).toSet()
    }

    private fun normalizeRoleName(name: String): String =
        name.trim().uppercase()

    private fun roleNameConflict(name: String): ConflictException =
        ConflictException(
            code = "ROLE_NAME_ALREADY_EXISTS",
            message = "Role already exists: $name.",
        )

    private fun toResponse(role: Role): RoleResponse =
        RoleResponse(
            id = requireNotNull(role.id),
            name = role.name,
            permissions = role.permissions.map { it.name }.distinct().sorted(),
        )

    private fun roleSnapshot(role: Role): Map<String, Any?> =
        mapOf(
            "id" to role.id,
            "name" to role.name,
            "permissions" to role.permissions.map { it.name }.distinct().sorted(),
        )

    private fun json(value: Any): String =
        jsonMapper.writeValueAsString(value)
}
