package com.uade.dda2.server.feature.auth

import com.uade.dda2.server.feature.auth.dto.M1LocalAccount
import com.uade.dda2.server.feature.auth.entity.CitizenSnapshot
import com.uade.dda2.server.feature.auth.entity.Permission
import com.uade.dda2.server.feature.auth.entity.Role
import com.uade.dda2.server.feature.auth.entity.User
import com.uade.dda2.server.feature.auth.repository.CitizenSnapshotRepository
import com.uade.dda2.server.feature.auth.repository.PermissionRepository
import com.uade.dda2.server.feature.auth.repository.RoleRepository
import com.uade.dda2.server.feature.auth.repository.UserRepository
import com.uade.dda2.server.feature.auth.service.M1CitizenAccountService
import com.uade.dda2.server.feature.integration.dto.M1CitizenIdentity
import com.uade.dda2.server.feature.integration.error.M1AuthException
import com.uade.dda2.server.feature.integration.error.M1AuthFailure
import com.uade.dda2.server.feature.log.entity.LogAction
import com.uade.dda2.server.feature.log.mapper.LogMapper
import com.uade.dda2.server.feature.log.repository.LogRepository
import com.uade.dda2.server.feature.log.service.LogService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.mock
import org.mockito.stubbing.Answer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.json.JsonMapper
import java.lang.reflect.InvocationTargetException
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.*

@DataJpaTest(showSql = false, properties = [
    "spring.test.database.replace=NONE",
    "spring.datasource.url=jdbc:h2:mem:m1-accounts;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=VALUE;LOCK_TIMEOUT=10000",
    "spring.datasource.hikari.maximum-pool-size=12",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true",
    "spring.jpa.show-sql=false",
])
@ActiveProfiles("docs")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import(M1CitizenAccountService::class, LogService::class, LogMapper::class, M1AccountTestConfiguration::class)
class M1CitizenAccountServiceTest {
    @Autowired lateinit var accounts: M1CitizenAccountService
    @Autowired lateinit var users: UserRepository
    @Autowired lateinit var roles: RoleRepository
    @Autowired lateinit var permissions: PermissionRepository
    @Autowired lateinit var snapshots: CitizenSnapshotRepository
    @Autowired lateinit var audit: LogRepository
    @Autowired lateinit var logs: LogService
    @Autowired lateinit var json: JsonMapper
    @Autowired lateinit var manager: PlatformTransactionManager
    private val email = "ciudadano@example.invalid"
    private lateinit var identity: M1CitizenIdentity

    private fun <T> tx(action: () -> T): T = requireNotNull(TransactionTemplate(manager).execute { action() })

    @BeforeEach fun setup() {
        identity = M1CitizenIdentity(UUID.randomUUID(), 8452L, "Marcos Ferreyra")
        tx {
            audit.deleteAll()
            users.deleteAll()
            roles.deleteAll()
            permissions.deleteAll()
            permissions.flush()
            val own = permissions.save(Permission(name = "applications:own:view"))
            roles.save(Role(name = "CIUDADANO", permissions = mutableSetOf(own)))
            roles.save(Role(name = "ADMIN"))
        }
    }

    private fun linked(active: Boolean = true, citizenRole: Boolean = true, externalId: String = identity.citizenId.toString()): Long = tx {
        val assigned = roles.findByNameIn(if (citizenRole) listOf("CIUDADANO", "ADMIN") else listOf("ADMIN"))
        requireNotNull(users.saveAndFlush(User(username = "local", passwordHash = "existing-local-hash", name = "Nombre local",
            email = "local@example.invalid", externalCitizenId = externalId, active = active, roles = assigned.toMutableSet())).id)
    }

    @Test fun `primer ingreso crea identidad externa con UUID canonico y solo rol ciudadano`() {
        val result = accounts.resolve(identity, " $email ")
        assertTrue(result.created)
        assertEquals(identity.citizenId, result.citizenId)
        tx {
            val user = users.findByIdWithRoles(result.userId)!!
            assertEquals(identity.citizenId.toString(), user.externalCitizenId)
            assertNull(user.username)
            assertNull(user.passwordHash)
            assertEquals("Marcos Ferreyra", user.name)
            assertEquals(email, user.email)
            assertTrue(user.active)
            assertEquals(setOf("CIUDADANO"), user.roles.map { it.name }.toSet())
            assertEquals(setOf("applications:own:view"), user.roles.single().permissions.map { it.name }.toSet())
            assertFalse(snapshots.existsById(result.userId))
            val record = audit.findAll().single()
            assertEquals(LogAction.CREATE, record.action)
            assertEquals(result.userId.toString(), record.entityId)
            assertEquals(result.userId, record.user?.id)
            assertTrue(record.newValues.orEmpty().contains(identity.citizenId.toString()))
            assertFalse(record.newValues.orEmpty().contains(email))
            assertFalse(record.newValues.orEmpty().contains("password"))
        }
    }

    @Test fun `ingresos sucesivos conservan el usuario local sin reescribir perfil ni duplicar auditoria`() {
        val first = accounts.resolve(identity, email)
        val next = accounts.resolve(identity.copy(userId = 9999L, name = "Otro nombre M1"), "otro@example.invalid")
        assertEquals(first.userId, next.userId)
        assertFalse(next.created)
        tx {
            assertEquals(1, users.count())
            val user = users.findById(first.userId).orElseThrow()
            assertEquals("Marcos Ferreyra", user.name)
            assertEquals(email, user.email)
            assertNull(user.updatedAt)
            assertEquals(1, audit.count())
        }
    }

    @Test fun `reutiliza vinculo en mayusculas y preserva credenciales y roles administrativos existentes`() {
        val id = linked(externalId = identity.citizenId.toString().uppercase())
        val result = accounts.resolve(identity, email)
        assertEquals(id, result.userId)
        assertFalse(result.created)
        tx {
            val user = users.findByIdWithRoles(id)!!
            assertEquals("local", user.username)
            assertEquals("existing-local-hash", user.passwordHash)
            assertEquals("Nombre local", user.name)
            assertEquals("local@example.invalid", user.email)
            assertEquals(setOf("CIUDADANO", "ADMIN"), user.roles.map { it.name }.toSet())
            assertEquals(0, audit.count())
        }
    }

    @Test fun `cuenta inactiva no se reactiva ni se reemplaza con otra`() {
        val id = linked(active = false)
        assertEquals(M1AuthFailure.LOCAL_ACCOUNT_INACTIVE, assertFailsWith<M1AuthException> { accounts.resolve(identity, email) }.failure)
        tx { assertFalse(users.findById(id).orElseThrow().active); assertEquals(1, users.count()); assertEquals(0, audit.count()) }
    }

    @Test fun `rol ciudadano retirado no se reasigna automaticamente`() {
        val id = linked(citizenRole = false)
        assertEquals(M1AuthFailure.CITIZEN_ROLE_REQUIRED, assertFailsWith<M1AuthException> { accounts.resolve(identity, email) }.failure)
        tx { assertEquals(setOf("ADMIN"), users.findByIdWithRoles(id)!!.roles.map { it.name }.toSet()); assertEquals(0, audit.count()) }
    }

    @Test fun `sin rol ciudadano configurado no crea cuentas ni roles improvisados`() {
        tx { roles.deleteAll() }
        assertEquals(M1AuthFailure.CITIZEN_ROLE_UNAVAILABLE, assertFailsWith<M1AuthException> { accounts.resolve(identity, email) }.failure)
        tx { assertEquals(0, users.count()); assertEquals(0, roles.count()); assertEquals(0, audit.count()) }
    }

    @Test fun `correo nombre DNI y user id numerico no fusionan cuentas`() {
        val old = tx {
            val user = users.saveAndFlush(User(username = "previo", passwordHash = "hash-previo", name = identity.name, email = email))
            snapshots.saveAndFlush(CitizenSnapshot(user = user, dni = "34567890", email = email))
            requireNotNull(user.id)
        }
        val result = accounts.resolve(identity.copy(userId = old), email)
        assertNotEquals(old, result.userId)
        tx {
            val previous = users.findById(old).orElseThrow()
            assertNull(previous.externalCitizenId)
            assertEquals("hash-previo", previous.passwordHash)
            assertEquals("34567890", snapshots.findById(old).orElseThrow().dni)
            assertEquals(2, users.count())
        }
    }

    @Test fun `mismo correo con distintos UUID crea identidades locales independientes`() {
        val first = accounts.resolve(identity, email)
        val other = accounts.resolve(identity.copy(citizenId = UUID.randomUUID()), email)
        assertNotEquals(first.userId, other.userId)
        assertTrue(other.created)
        tx { assertEquals(2, users.count()); assertEquals(2, audit.count()) }
    }

    @Test fun `snapshot bloqueado o fallecido se conserva sin modificar la politica de nuevas solicitudes`() {
        val id = linked()
        tx {
            snapshots.saveAndFlush(CitizenSnapshot(user = users.getReferenceById(id), blocked = true,
                blockReason = "Restricción M1", reviewRequired = true, deceasedAt = LocalDate.of(2026, 10, 1)))
        }
        assertEquals(id, accounts.resolve(identity, email).userId)
        tx {
            val snapshot = snapshots.findById(id).orElseThrow()
            assertTrue(snapshot.blocked)
            assertTrue(snapshot.reviewRequired)
            assertEquals("Restricción M1", snapshot.blockReason)
            assertEquals(LocalDate.of(2026, 10, 1), snapshot.deceasedAt)
        }
    }

    @Test fun `vinculos legados duplicados por mayusculas requieren revision y no se elige uno`() {
        linked()
        tx { users.saveAndFlush(User(name = "Duplicado", email = email, externalCitizenId = identity.citizenId.toString().uppercase())) }
        assertEquals(M1AuthFailure.IDENTITY_CONFLICT, assertFailsWith<M1AuthException> { accounts.resolve(identity, email) }.failure)
        tx { assertEquals(2, users.count()); assertEquals(0, audit.count()) }
    }

    @ParameterizedTest
    @ValueSource(strings = ["empty-email", "long-email", "empty-name", "long-name"])
    fun `datos fuera de los limites locales no dejan altas parciales`(scenario: String) {
        val profile = when (scenario) {
            "empty-name" -> identity.copy(name = " ")
            "long-name" -> identity.copy(name = "a".repeat(151))
            else -> identity
        }
        val submitted = when (scenario) { "empty-email" -> " "; "long-email" -> "a".repeat(181); else -> email }
        val expected = if (scenario.endsWith("email")) M1AuthFailure.INVALID_EMAIL else M1AuthFailure.INVALID_RESPONSE
        assertEquals(expected, assertFailsWith<M1AuthException> { accounts.resolve(profile, submitted) }.failure)
        tx { assertEquals(0, users.count()); assertEquals(0, audit.count()) }
    }

    @Test fun `fallo de auditoria revierte el usuario y su asignacion de rol`() {
        val broken = mock(LogService::class.java, Answer { throw IllegalStateException("audit unavailable") })
        val isolated = M1CitizenAccountService(users, roles, broken, json, manager)
        assertFailsWith<IllegalStateException> { isolated.resolve(identity, email) }
        tx { assertEquals(0, users.count()); assertEquals(0, audit.count()) }
        assertTrue(accounts.resolve(identity, email).created)
    }

    @Test fun `integridad fallida sin otra cuenta ganadora devuelve error local sin conservar la causa`() {
        val broken = mock(LogService::class.java, Answer { throw DataIntegrityViolationException("private database detail") })
        val isolated = M1CitizenAccountService(users, roles, broken, json, manager)
        val error = assertFailsWith<M1AuthException> { isolated.resolve(identity, email) }
        assertEquals(M1AuthFailure.PROVISIONING_FAILED, error.failure)
        assertNull(error.cause)
        assertFalse(error.stackTraceToString().contains("private database detail"))
        tx { assertEquals(0, users.count()); assertEquals(0, audit.count()) }
    }

    @Test fun `la cuenta confirmada es independiente de una transaccion exterior que se revierte`() {
        lateinit var result: M1LocalAccount
        TransactionTemplate(manager).execute { outer ->
            result = accounts.resolve(identity, email)
            outer.setRollbackOnly()
        }
        tx { assertTrue(users.existsById(result.userId)); assertEquals(1, audit.count()) }
    }

    @Test fun `primeros ingresos simultaneos desde dos instancias recuperan una unica cuenta y auditoria`() {
        val parallelism = 8
        val barrier = CyclicBarrier(parallelism)
        val reads = AtomicInteger()
        // Delega a JPA real y obliga a que todos lean "no existe" antes de insertar.
        val synchronizedUsers = mock(UserRepository::class.java, Answer { invocation ->
            val result = try { invocation.method.invoke(users, *invocation.arguments) }
            catch (exception: InvocationTargetException) { throw exception.targetException }
            if (invocation.method.name == "findIdsByExternalCitizenIdIgnoreCase" && reads.incrementAndGet() <= parallelism)
                barrier.await(10, TimeUnit.SECONDS)
            result
        })
        val instances = List(2) { M1CitizenAccountService(synchronizedUsers, roles, logs, json, manager) }
        val pool = Executors.newFixedThreadPool(parallelism)
        try {
            val futures = (0 until parallelism).map { index ->
                pool.submit<M1LocalAccount> { instances[index % 2].resolve(identity, email) }
            }
            val results = futures.map { it.get(20, TimeUnit.SECONDS) }
            assertEquals(1, results.map { it.userId }.distinct().size)
            assertEquals(1, results.count { it.created })
            tx {
                assertEquals(1, users.count())
                assertEquals(1, users.findByIdWithRoles(results.first().userId)!!.roles.size)
                assertEquals(1, audit.count())
                assertEquals(LogAction.CREATE, audit.findAll().single().action)
            }
        } finally {
            pool.shutdownNow()
            assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS))
        }
    }
}

@TestConfiguration(proxyBeanMethods = false)
class M1AccountTestConfiguration {
    @Bean fun m1AccountJson(): JsonMapper = JsonMapper.builder().findAndAddModules().build()
}
