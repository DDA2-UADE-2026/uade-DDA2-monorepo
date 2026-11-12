package com.uade.dda2.server.feature.application.validator

import com.uade.dda2.server.error.ApiException
import com.uade.dda2.server.feature.application.dto.request.CreateApplicationEvaluationRequest
import com.uade.dda2.server.feature.application.entity.*
import com.uade.dda2.server.feature.auth.entity.*
import com.uade.dda2.server.feature.enrollmentperiod.entity.*
import com.uade.dda2.server.feature.program.entity.ProgramEdition
import com.uade.dda2.server.feature.program.entity.enums.ProgramEditionStatus
import com.uade.dda2.server.security.JwtPrincipal
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.mockito.Mockito.*
import java.time.LocalDate
import kotlin.test.*

class ApplicationDomainMutationTest {
    private val validator = ApplicationValidator()
    private val evaluation = ApplicationEvaluationValidator()
    private fun reject(code: String, action: () -> Unit) =
        assertEquals(code, assertFailsWith<ApiException>(block = action).code)

    @TestFactory
    fun `actors must be active and authorized by role and permission`() =
        listOf("application", "evaluation", "visit").flatMap { kind ->
            listOf("valid", "null", "inactive", "role", "token", "database").filter { kind != "application" || it != "database" }.map { scenario ->
                dynamicTest("$kind $scenario") {
                    val role = Role(name = "WORKER", permissions = mutableSetOf(Permission(name = "other"), Permission(name = "evaluate")))
                    val user = User(id = 2, roles = mutableSetOf(Role(name = "OTHER"), role))
                    var principal = JwtPrincipal(2, "qa", "WORKER", listOf("evaluate"))
                    when (scenario) {
                        "inactive" -> user.active = false
                        "role" -> user.roles.clear()
                        "token" -> principal = principal.copy(permissions = emptyList())
                        "database" -> role.permissions.clear()
                    }
                    val candidate = if (scenario == "null") null else user
                    val action = {
                        when (kind) {
                            "application" -> validator.validateUser(candidate, principal, "evaluate")
                            "evaluation" -> evaluation.validateActor(candidate, principal, "evaluate")
                            else -> ApplicationVisitValidator().validateActor(candidate, principal, "evaluate")
                        }
                    }
                    if (scenario == "valid") assertSame(user, action())
                    else reject(if (scenario in setOf("null", "inactive")) "AUTH_UNAUTHENTICATED" else "AUTH_FORBIDDEN") { action() }
                }
            }
        }

    @Test
    fun `submission includes both date boundaries and requires open period and active edition`() {
        val period = mock(EnrollmentPeriod::class.java)
        val edition = mock(ProgramEdition::class.java)
        val start = LocalDate.of(2026, 1, 1)
        val end = start.plusDays(10)
        `when`(period.programEdition).thenReturn(edition)
        `when`(period.status).thenReturn(EnrollmentPeriodStatus.OPEN)
        `when`(period.openDate).thenReturn(start)
        `when`(period.closeDate).thenReturn(end)
        `when`(edition.status).thenReturn(ProgramEditionStatus.ACTIVE)
        for (day in listOf(start, start.plusDays(1), end)) validator.validatePeriod(period, day)
        for (day in listOf(start.minusDays(1), end.plusDays(1))) reject("APPLICATION_OUTSIDE_ENROLLMENT_PERIOD") { validator.validatePeriod(period, day) }
        for (state in EnrollmentPeriodStatus.entries.filter { it != EnrollmentPeriodStatus.OPEN }) {
            `when`(period.status).thenReturn(state)
            reject("APPLICATION_ENROLLMENT_PERIOD_NOT_OPEN") { validator.validatePeriod(period, start) }
        }
        `when`(period.status).thenReturn(EnrollmentPeriodStatus.OPEN)
        for (state in ProgramEditionStatus.entries.filter { it != ProgramEditionStatus.ACTIVE }) {
            `when`(edition.status).thenReturn(state)
            reject("APPLICATION_PROGRAM_EDITION_NOT_ACTIVE") { validator.validatePeriod(period, start) }
        }
    }

    @Test
    fun `optional idempotency keys enforce length and visible ASCII boundaries`() {
        listOf(null, "!", "~", "a".repeat(128)).forEach(validator::validateIdempotencyKey)
        listOf("", "a".repeat(129), "has space", "\n", "\u007f", "á").forEach { key ->
            reject("APPLICATION_INVALID_IDEMPOTENCY_KEY") { validator.validateIdempotencyKey(key) }
        }
    }

    @TestFactory
    fun `evaluation accepts only eligible application stages`() = ApplicationStatus.entries.map { status -> dynamicTest(status.name) {
        val application = mock(Application::class.java)
        val professional = User(id = 2)
        `when`(application.status).thenReturn(status)
        `when`(application.user).thenReturn(User(id = 1))
        if (status in setOf(ApplicationStatus.SUBMITTED, ApplicationStatus.IN_VALIDATION, ApplicationStatus.PENDING_DOCUMENTATION, ApplicationStatus.IN_EVALUATION))
            evaluation.validateApplication(application, professional)
        else reject("APPLICATION_EVALUATION_STATUS_NOT_ALLOWED") { evaluation.validateApplication(application, professional) }
    } }

    @Test
    fun `evaluation prevents self review and respects the assigned professional`() {
        val application = mock(Application::class.java)
        val actor = User(id = 2)
        `when`(application.status).thenReturn(ApplicationStatus.IN_EVALUATION)
        `when`(application.user).thenReturn(User(id = 1))
        `when`(application.assignedWorker).thenReturn(actor)
        evaluation.validateApplication(application, actor)
        `when`(application.assignedWorker).thenReturn(User(id = 3))
        reject("APPLICATION_EVALUATION_ASSIGNED_TO_ANOTHER_WORKER") { evaluation.validateApplication(application, actor) }
        `when`(application.user).thenReturn(actor)
        reject("APPLICATION_EVALUATION_OWN_APPLICATION") { evaluation.validateApplication(application, actor) }
    }

    @Test
    fun `evaluation normalizes recommendation and requires both classifications`() {
        val vulnerability = VulnerabilityLevel.entries.first()
        val priority = EvaluationPriority.entries.first()
        val valid = CreateApplicationEvaluationRequest(vulnerability, priority, " recommendation ")
        val result = evaluation.validateRequest(valid)
        assertEquals(vulnerability, result.vulnerability)
        assertEquals(priority, result.priority)
        assertEquals("recommendation", result.recommendation)
        assertEquals(1000, evaluation.validateRequest(valid.copy(recommendation = "x".repeat(1000))).recommendation.length)
        val invalid = listOf(valid.copy(vulnerability = null), valid.copy(priority = null)) +
            listOf(null, "", " ", "x".repeat(1001)).map { valid.copy(recommendation = it) }
        invalid.forEach { request -> reject("APPLICATION_EVALUATION_INVALID_DATA") { evaluation.validateRequest(request) } }
    }
}
