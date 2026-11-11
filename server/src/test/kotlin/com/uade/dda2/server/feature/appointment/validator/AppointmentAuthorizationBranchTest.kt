package com.uade.dda2.server.feature.appointment.validator

import com.uade.dda2.server.error.ApiException
import com.uade.dda2.server.feature.auth.entity.*
import com.uade.dda2.server.security.JwtPrincipal
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.DynamicTest.dynamicTest
import java.time.OffsetDateTime
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class AppointmentAuthorizationBranchTest {
    private val validator = AppointmentValidator()
    private val permission = "appointments:read"
    private fun check(role: String, user: User?, principal: JwtPrincipal): User = when (role) {
        "CIUDADANO" -> validator.validateCitizen(user, principal, permission)
        "ADMIN" -> validator.validateAdmin(user, principal, permission)
        else -> validator.validateProfessional(user, principal, permission)
    }
    private fun reject(code: String, action: () -> Unit) =
        assertEquals(code, assertFailsWith<ApiException>(block = action).code)

    @TestFactory
    fun `authorization requires active user matching role and permissions in token and database`() =
        listOf("CIUDADANO", "ADMIN", "PROFESIONAL_CENTRO").flatMap { role ->
            listOf("valid", "missing user", "inactive", "wrong active role", "missing role", "missing token permission", "missing database permission").map { scenario ->
                dynamicTest("$role - $scenario") {
                    val assigned = Role(name = role.lowercase(), permissions = mutableSetOf(Permission(name = "unrelated"), Permission(name = permission)))
                    val user = User(id = 1, username = "qa", roles = mutableSetOf(Role(name = "OTHER"), assigned))
                    var principal = JwtPrincipal(1, "qa", role, listOf(permission))
                    when (scenario) {
                        "inactive" -> user.active = false
                        "wrong active role" -> principal = principal.copy(activeRole = "UNASSIGNED")
                        "missing role" -> user.roles.clear()
                        "missing token permission" -> principal = principal.copy(permissions = emptyList())
                        "missing database permission" -> assigned.permissions.clear()
                    }
                    val actualUser = if (scenario == "missing user") null else user
                    when (scenario) {
                        "valid" -> assertSame(user, check(role, actualUser, principal))
                        "missing user", "inactive" -> reject("AUTH_UNAUTHENTICATED") { check(role, actualUser, principal) }
                        else -> reject("AUTH_FORBIDDEN") { check(role, actualUser, principal) }
                    }
                }
            }
        }

    @TestFactory
    fun `idempotency keys accept visible ASCII only within length boundaries`() =
        listOf("!" to true, "~".repeat(128) to true, "" to false, "x".repeat(129) to false,
            "has space" to false, "line\n" to false, "á" to false, "\u007f" to false).map { (key, valid) ->
            dynamicTest("key length ${key.length} valid=$valid chars=${key.map { it.code }}") {
                if (valid) validator.validateIdempotencyKey(key)
                else reject("APPOINTMENT_INVALID_IDEMPOTENCY_KEY") { validator.validateIdempotencyKey(key) }
            }
        }

    @Test
    fun `date validation uses the center timezone at day boundaries`() {
        val now = OffsetDateTime.parse("2026-06-02T01:00:00Z")
        val zone = ZoneId.of("America/Argentina/Buenos_Aires")
        validator.validateDate(java.time.LocalDate.of(2026, 6, 1), now, zone)
        reject("APPOINTMENT_TIME_IN_PAST") { validator.validateDate(java.time.LocalDate.of(2026, 5, 31), now, zone) }
    }

    @Test
    fun `requested ranges must be ordered and strictly in the future`() {
        val now = OffsetDateTime.parse("2026-06-01T10:00:00Z")
        validator.validateRequestedRange(now.plusSeconds(1), now.plusHours(1), now)
        reject("APPOINTMENT_INVALID_REQUEST") { validator.validateRequestedRange(now, now, now) }
        reject("APPOINTMENT_INVALID_REQUEST") { validator.validateRequestedRange(now.plusHours(1), now, now) }
        reject("APPOINTMENT_TIME_IN_PAST") { validator.validateRequestedRange(now, now.plusHours(1), now) }
        reject("APPOINTMENT_TIME_IN_PAST") { validator.validateRequestedRange(now.minusSeconds(1), now.plusHours(1), now) }
    }
}
