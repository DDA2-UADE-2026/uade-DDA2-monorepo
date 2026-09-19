package com.uade.dda2.server.feature.auth.service

import tools.jackson.databind.json.JsonMapper
import com.uade.dda2.server.error.BadRequestException
import com.uade.dda2.server.error.ConflictException
import com.uade.dda2.server.error.NotFoundException
import com.uade.dda2.server.feature.auth.dto.request.CreateUserRequest
import com.uade.dda2.server.feature.auth.dto.request.UpdateUserRequest
import com.uade.dda2.server.feature.auth.dto.response.UserManagementResponse
import com.uade.dda2.server.feature.auth.repository.RoleRepository
import com.uade.dda2.server.feature.auth.repository.UserRepository
import com.uade.dda2.server.feature.application.repository.ApplicationRepository
import com.uade.dda2.server.feature.application.repository.ApplicationDocumentRepository
import com.uade.dda2.server.feature.activity.repository.ActivityRepository
import com.uade.dda2.server.feature.activity.repository.ActivityEnrollmentRepository
import com.uade.dda2.server.feature.center.repository.ProfessionalAssignmentRepository
import com.uade.dda2.server.feature.center.validator.CenterLifecycleValidator
import com.uade.dda2.server.feature.document.repository.DocumentRepository
import com.uade.dda2.server.feature.log.entity.LogAction
import com.uade.dda2.server.feature.log.entity.LogEntityType
import com.uade.dda2.server.feature.log.repository.LogRepository
import com.uade.dda2.server.feature.log.service.LogService
import com.uade.dda2.server.feature.auth.entity.Role
import com.uade.dda2.server.feature.auth.entity.User
import com.uade.dda2.server.feature.program.repository.ProgramEditionRepository
import com.uade.dda2.server.feature.program.repository.ProgramRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class UserManagementService(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val logRepository: LogRepository,
    private val currentUserService: CurrentUserService,
    private val logService: LogService,
    private val passwordEncoder: PasswordEncoder,
    private val jsonMapper: JsonMapper,
    private val programRepository: ProgramRepository,
    private val programEditionRepository: ProgramEditionRepository,
    private val activityRepository: ActivityRepository,
    private val activityEnrollmentRepository: ActivityEnrollmentRepository,
    private val applicationRepository: ApplicationRepository,
    private val applicationDocumentRepository: ApplicationDocumentRepository,
    private val documentRepository: DocumentRepository,
    private val professionalAssignmentRepository: ProfessionalAssignmentRepository,
    private val centerLifecycleValidator: CenterLifecycleValidator,
) {
    @Transactional(readOnly = true)
    fun findAll(): List<UserManagementResponse> =
        userRepository.findAllByOrderByUsernameAsc().map(::toResponse)

    @Transactional(readOnly = true)
    fun findById(id: Long): UserManagementResponse =
        toResponse(findUser(id))

    @Transactional
    fun create(request: CreateUserRequest): UserManagementResponse {
        val username = normalizeUsername(request.username)
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw usernameConflict(username)
        }

        val user = userRepository.save(
            User(
                username = username,
                passwordHash = requireNotNull(passwordEncoder.encode(request.password)),
                name = request.name.trim(),
                email = request.email.trim(),
                active = request.active,
                roles = resolveRoles(request.roles).toMutableSet(),
            ),
        )
        val response = toResponse(user)
        logService.record(
            user = currentUserService.userReference(),
            action = LogAction.CREATE,
            entityType = LogEntityType.USER,
            entityId = requireNotNull(user.id).toString(),
            newValues = json(userSnapshot(user)),
        )

        return response
    }

    @Transactional
    fun update(id: Long, request: UpdateUserRequest): UserManagementResponse {
        val resolvedRoles = resolveRoles(request.roles)
        val willHaveProfessionalRole = resolvedRoles.any { it.name.equals("PROFESIONAL_CENTRO", ignoreCase = true) }
        if (request.active && willHaveProfessionalRole) {
            centerLifecycleValidator.lockProfessionalActivation(id)
        }
        val user = findUserForUpdate(id)
        val username = request.username?.let(::normalizeUsername) ?: user.username
        val passwordHash = request.password?.let { requireNotNull(passwordEncoder.encode(it)) } ?: user.passwordHash
        if (username?.isBlank() == true || (username == null) != (passwordHash == null)) {
            throw BadRequestException(
                "USER_INCOMPLETE_LOCAL_CREDENTIALS",
                "Para habilitar el acceso local se requieren username y password juntos.",
            )
        }
        if (username != null && userRepository.existsByUsernameIgnoreCaseAndIdNot(username, id)) {
            throw usernameConflict(username)
        }

        val hadProfessionalRole = user.hasRole("PROFESIONAL_CENTRO")
        if (hadProfessionalRole && !willHaveProfessionalRole && professionalAssignmentRepository.existsByProfessionalIdAndActiveTrue(id)) {
            throw ConflictException(
                code = "PROFESSIONAL_ROLE_HAS_ACTIVE_ASSIGNMENTS",
                message = "No se puede retirar PROFESIONAL_CENTRO mientras el usuario tenga asignaciones activas.",
            )
        }

        val wasActive = user.active
        val oldValues = json(userSnapshot(user))
        user.username = username
        user.passwordHash = passwordHash
        user.name = request.name.trim()
        user.email = request.email.trim()
        user.active = request.active
        user.updatedAt = Instant.now()
        user.roles.clear()
        user.roles.addAll(resolvedRoles)

        if (request.active && willHaveProfessionalRole && (!wasActive || !hadProfessionalRole)) {
            centerLifecycleValidator.validateProfessionalActivation(id)
        }

        val response = toResponse(user)
        logService.record(
            user = currentUserService.userReference(),
            action = LogAction.UPDATE,
            entityType = LogEntityType.USER,
            entityId = id.toString(),
            oldValues = oldValues,
            newValues = json(userSnapshot(user)),
        )

        return response
    }

    @Transactional
    fun delete(id: Long) {
        val currentPrincipal = currentUserService.principal()
        if (currentPrincipal.id == id) {
            throw BadRequestException(
                code = "USER_SELF_DELETE_NOT_ALLOWED",
                message = "The current user cannot delete itself.",
            )
        }

        val user = findUser(id)

        if (
            applicationRepository.existsByUserIdOrAssignedWorkerIdOrRegisteredById(id, id, id) ||
            applicationDocumentRepository.existsByReviewedById(id) ||
            documentRepository.existsByUploadedById(id)
        ) {
            throw ConflictException("USER_HAS_APPLICATION_REFERENCES", "No se puede eliminar un usuario vinculado a solicitudes o sus documentos.")
        }

        if (
            programRepository.existsByCreatedById(id) ||
            programEditionRepository.existsByCreatedById(id)
        ) {
            throw ConflictException(
                code = "USER_HAS_PROGRAM_REFERENCES",
                message = "The user cannot be deleted because it created programs or program editions.",
            )
        }

        if (
            activityRepository.existsByCreatedById(id) ||
            activityEnrollmentRepository.existsByCitizenIdOrAttendanceRecordedById(id, id)
        ) {
            throw ConflictException(
                code = "USER_HAS_ACTIVITY_REFERENCES",
                message = "No se puede eliminar un usuario vinculado a actividades comunitarias o sus inscripciones.",
            )
        }

        if (professionalAssignmentRepository.existsByProfessionalId(id)) {
            throw ConflictException(
                code = "USER_HAS_PROFESSIONAL_ASSIGNMENTS",
                message = "No se puede eliminar un usuario con asignaciones profesionales.",
            )
        }

        val oldValues = json(userSnapshot(user))

        user.roles.clear()
        logService.record(
            user = currentUserService.userReference(),
            action = LogAction.DELETE,
            entityType = LogEntityType.USER,
            entityId = id.toString(),
            oldValues = oldValues,
        )
        logRepository.clearUserReference(id)
        userRepository.delete(user)
    }

    private fun findUser(id: Long): User =
        userRepository.findByIdWithRoles(id)
            ?: throw NotFoundException(
                code = "USER_NOT_FOUND",
                message = "User not found.",
            )

    private fun findUserForUpdate(id: Long): User =
        userRepository.findByIdForUpdate(id)
            ?: throw NotFoundException(
                code = "USER_NOT_FOUND",
                message = "User not found.",
            )

    private fun User.hasRole(name: String): Boolean =
        roles.any { it.name.equals(name, ignoreCase = true) }

    private fun resolveRoles(roleNames: List<String>): Set<Role> {
        val names = roleNames.map(::normalizeRoleName).filter(String::isNotBlank).distinct().toSet()
        if (names.size != roleNames.size) {
            throw BadRequestException(
                code = "USER_INVALID_ROLES",
                message = "Roles must not contain blank or duplicated values.",
            )
        }
        if (names.isEmpty()) {
            return emptySet()
        }

        val roles = roleRepository.findByNameIn(names).associateBy { it.name.uppercase() }
        val missing = names.filterNot(roles::containsKey)
        if (missing.isNotEmpty()) {
            throw BadRequestException(
                code = "USER_UNKNOWN_ROLES",
                message = "Unknown roles: ${missing.sorted().joinToString(", ")}.",
            )
        }

        return names.mapNotNull(roles::get).toSet()
    }

    private fun normalizeUsername(username: String): String =
        username.trim()

    private fun normalizeRoleName(name: String): String =
        name.trim().uppercase()

    private fun usernameConflict(username: String): ConflictException =
        ConflictException(
            code = "USER_USERNAME_ALREADY_EXISTS",
            message = "Username already exists: $username.",
        )

    private fun toResponse(user: User): UserManagementResponse =
        UserManagementResponse(
            id = requireNotNull(user.id),
            username = user.username,
            name = user.name,
            email = user.email,
            active = user.active,
            roles = user.roles.map { it.name }.distinct().sorted(),
            permissionsByRole = permissionsByRole(user),
            hasLocalCredentials = user.username != null && user.passwordHash != null,
            externalCitizenId = user.externalCitizenId,
            createdAt = user.createdAt,
            updatedAt = user.updatedAt,
        )

    private fun userSnapshot(user: User): Map<String, Any?> =
        mapOf(
            "id" to user.id,
            "username" to user.username,
            "name" to user.name,
            "email" to user.email,
            "active" to user.active,
            "roles" to user.roles.map { it.name }.distinct().sorted(),
            "permissionsByRole" to permissionsByRole(user),
            "createdAt" to user.createdAt.toString(),
            "updatedAt" to user.updatedAt?.toString(),
        )

    private fun json(value: Any): String =
        requireNotNull(jsonMapper.writeValueAsString(value))

    private fun permissionsByRole(user: User): Map<String, List<String>> =
        user.roles.sortedBy { it.name }.associate { role ->
            role.name to role.permissions.map { it.name }.distinct().sorted()
        }
}
