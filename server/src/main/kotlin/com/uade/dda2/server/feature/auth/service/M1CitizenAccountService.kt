package com.uade.dda2.server.feature.auth.service

import com.uade.dda2.server.feature.auth.dto.M1LocalAccount
import com.uade.dda2.server.feature.auth.entity.User
import com.uade.dda2.server.feature.auth.repository.RoleRepository
import com.uade.dda2.server.feature.auth.repository.UserRepository
import com.uade.dda2.server.feature.integration.dto.M1CitizenIdentity
import com.uade.dda2.server.feature.integration.error.M1AuthException
import com.uade.dda2.server.feature.integration.error.M1AuthFailure
import com.uade.dda2.server.feature.log.entity.LogAction
import com.uade.dda2.server.feature.log.entity.LogEntityType
import com.uade.dda2.server.feature.log.service.LogService
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.json.JsonMapper

@Service
class M1CitizenAccountService(
    private val users: UserRepository,
    private val roles: RoleRepository,
    private val logs: LogService,
    private val json: JsonMapper,
    transactionManager: PlatformTransactionManager,
) {
    private val transaction = TransactionTemplate(transactionManager).apply {
        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
    }

    /** Invocar solo con la identidad devuelta por M1AuthClient y el correo de ese mismo login. */
    fun resolve(identity: M1CitizenIdentity, authenticatedEmail: String): M1LocalAccount {
        val email = authenticatedEmail.trim()
        if (email.isEmpty() || email.length > 180) throw M1AuthException(M1AuthFailure.INVALID_EMAIL)
        val name = identity.name.trim()
        if (name.isEmpty() || name.length > 150) throw M1AuthException(M1AuthFailure.INVALID_RESPONSE)

        return try {
            requireNotNull(transaction.execute {
                existing(identity)?.let { return@execute it }
                val role = roles.findByNameIn(listOf(CITIZEN_ROLE)).singleOrNull()
                    ?: throw M1AuthException(M1AuthFailure.CITIZEN_ROLE_UNAVAILABLE)
                val user = users.saveAndFlush(User(
                    externalCitizenId = identity.citizenId.toString(), name = name, email = email,
                    roles = mutableSetOf(role),
                ))
                logs.record(user, LogAction.CREATE, LogEntityType.USER, requireNotNull(user.id).toString(),
                    newValues = json.writeValueAsString(mapOf("source" to "M1",
                        "externalCitizenId" to identity.citizenId.toString(), "roles" to listOf(CITIZEN_ROLE))))
                M1LocalAccount(requireNotNull(user.id), identity.citizenId, created = true)
            })
        } catch (_: DataIntegrityViolationException) {
            // La transacción perdedora ya terminó. No consultar dentro de una transacción
            // abortada por PostgreSQL ni repetir el alta o la autenticación remota.
            requireNotNull(transaction.execute {
                existing(identity) ?: throw M1AuthException(M1AuthFailure.PROVISIONING_FAILED)
            })
        }
    }

    private fun existing(identity: M1CitizenIdentity): M1LocalAccount? {
        val citizenId = identity.citizenId.toString()
        val ids = users.findIdsByExternalCitizenIdIgnoreCase(citizenId)
        if (ids.isEmpty()) return null
        if (ids.size != 1) throw M1AuthException(M1AuthFailure.IDENTITY_CONFLICT)
        // Primero se localiza la PK y luego se bloquea la fila, antes de cargar el estado y roles.
        val user = users.findByIdForUpdate(ids.single())
            ?: throw M1AuthException(M1AuthFailure.IDENTITY_CONFLICT)
        if (!citizenId.equals(user.externalCitizenId, ignoreCase = true))
            throw M1AuthException(M1AuthFailure.IDENTITY_CONFLICT)
        if (!user.active) throw M1AuthException(M1AuthFailure.LOCAL_ACCOUNT_INACTIVE)
        if (user.roles.none { it.name == CITIZEN_ROLE }) throw M1AuthException(M1AuthFailure.CITIZEN_ROLE_REQUIRED)
        return M1LocalAccount(requireNotNull(user.id), identity.citizenId, created = false)
    }

    private companion object {
        const val CITIZEN_ROLE = "CIUDADANO"
    }
}
