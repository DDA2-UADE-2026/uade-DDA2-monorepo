package com.uade.dda2.server.feature.application.validator

import com.uade.dda2.server.error.ApiException
import com.uade.dda2.server.feature.application.dto.request.*
import com.uade.dda2.server.feature.application.entity.*
import com.uade.dda2.server.feature.auth.entity.User
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.mockito.Mockito.*
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.test.*

class ApplicationVisitMutationTest {
    private val validator = ApplicationVisitValidator()
    private val now = OffsetDateTime.parse("2026-11-11T12:00:00Z")
    private fun reject(suffix: String, action: () -> Unit) =
        assertEquals("APPLICATION_VISIT_$suffix", assertFailsWith<ApiException>(block = action).code)

    @Test
    fun `visit schedule normalizes timezone precision location and computes end`() {
        val start = now.plusHours(1).withOffsetSameInstant(ZoneOffset.ofHours(-3)).withNano(123456789)
        val expected = now.plusHours(1).withNano(123456000)
        for (type in ApplicationVisitType.entries) {
            val result = validator.validateRequest(CreateApplicationVisitRequest(type, start, 1, " office "), now)
            assertEquals(type, result.type)
            assertEquals(expected, result.startsAt)
            assertEquals(expected.plusMinutes(1), result.endsAt)
            assertEquals("office", result.location)
        }
        val moved = validator.validateRescheduleRequest(RescheduleApplicationVisitRequest(start, 30, "x".repeat(300)), now)
        assertEquals(expected.plusMinutes(30), moved.endsAt)
        assertEquals(300, moved.location.length)
    }

    @Test
    fun `schedule rejects missing fields nonpositive durations past time and long locations`() {
        val valid = CreateApplicationVisitRequest(ApplicationVisitType.VISIT, now.plusHours(1), 30, "office")
        val invalid = listOf(valid.copy(type = null), valid.copy(startsAt = null), valid.copy(durationMinutes = null),
            valid.copy(durationMinutes = 0), valid.copy(durationMinutes = -1), valid.copy(startsAt = now),
            valid.copy(startsAt = now.minusSeconds(1)), valid.copy(startsAt = OffsetDateTime.MAX.withOffsetSameLocal(ZoneOffset.UTC))) +
            listOf(null, "", " ", "x".repeat(301)).map { valid.copy(location = it) }
        invalid.forEach { request -> reject("INVALID_DATA") { validator.validateRequest(request, now) } }
    }

    @Test
    fun `cancellation reason must be meaningful and bounded`() {
        assertEquals("reason", validator.validateCancellationRequest(CancelApplicationVisitRequest(" reason ")))
        assertEquals(1000, validator.validateCancellationRequest(CancelApplicationVisitRequest("x".repeat(1000))).length)
        listOf(null, "", " ", "x".repeat(1001)).forEach { reason ->
            reject("INVALID_DATA") { validator.validateCancellationRequest(CancelApplicationVisitRequest(reason)) }
        }
    }

    @Test
    fun `result requires final status and normalizes optional bounded professional notes`() {
        reject("RESULT_INVALID_DATA") { validator.validateResultRequest(RecordApplicationVisitResultRequest()) }
        for (status in ApplicationVisitResultStatus.entries) {
            val empty = validator.validateResultRequest(RecordApplicationVisitResultRequest(status))
            assertEquals(ApplicationVisitStatus.valueOf(status.name), empty.status)
            assertNull(empty.observations)
            assertNull(empty.conclusions)
            val result = validator.validateResultRequest(RecordApplicationVisitResultRequest(status, " note ", "x".repeat(2000)))
            assertEquals("note", result.observations)
            assertEquals("x".repeat(2000), result.conclusions)
            for (text in listOf("", " ", "x".repeat(2001))) {
                reject("RESULT_INVALID_DATA") { validator.validateResultRequest(RecordApplicationVisitResultRequest(status, text, null)) }
                reject("RESULT_INVALID_DATA") { validator.validateResultRequest(RecordApplicationVisitResultRequest(status, null, text)) }
            }
        }
    }

    @TestFactory
    fun `only evaluated applications in evaluation or visit stage can schedule visits`() = ApplicationStatus.entries.map { state -> dynamicTest(state.name) {
        val application = mock(Application::class.java)
        val actor = User(id = 2)
        `when`(application.status).thenReturn(state)
        `when`(application.assignedWorker).thenReturn(actor)
        if (state in setOf(ApplicationStatus.IN_EVALUATION, ApplicationStatus.IN_VISIT)) validator.validateApplication(application, actor, true)
        else reject("STATUS_NOT_ALLOWED") { validator.validateApplication(application, actor, true) }
        reject("EVALUATION_REQUIRED") { validator.validateApplication(application, actor, false) }
    } }

    @Test
    fun `unassigned and differently assigned applications cannot schedule visits`() {
        val application = mock(Application::class.java)
        `when`(application.status).thenReturn(ApplicationStatus.IN_EVALUATION)
        val actor = User(id = 2)
        reject("ASSIGNED_TO_ANOTHER_WORKER") { validator.validateApplication(application, actor, true) }
        `when`(application.assignedWorker).thenReturn(User(id = 3))
        reject("ASSIGNED_TO_ANOTHER_WORKER") { validator.validateApplication(application, actor, true) }
    }

    @TestFactory
    fun `managing and recording visits enforces ownership stages and temporal boundaries`() =
        listOf("manage", "result").flatMap { operation ->
            listOf("valid", "application owner", "visit owner", "application state", "visit state", "boundary", "past").map { scenario ->
                dynamicTest("$operation $scenario") {
                    val actor = User(id = 2)
                    val application = mock(Application::class.java)
                    val visit = mock(ApplicationVisit::class.java)
                    `when`(application.assignedWorker).thenReturn(if (scenario == "application owner") User(id = 3) else actor)
                    `when`(application.status).thenReturn(if (scenario == "application state") ApplicationStatus.APPROVED else ApplicationStatus.IN_VISIT)
                    `when`(visit.responsible).thenReturn(if (scenario == "visit owner") User(id = 3) else actor)
                    `when`(visit.status).thenReturn(if (scenario == "visit state") ApplicationVisitStatus.REALIZED else ApplicationVisitStatus.SCHEDULED)
                    `when`(visit.startsAt).thenReturn(when (scenario) { "boundary" -> now; "past" -> now.minusSeconds(1); else -> now.plusSeconds(1) })
                    `when`(visit.endsAt).thenReturn(when (scenario) { "boundary" -> now; "past" -> now.plusSeconds(1); else -> now.minusSeconds(1) })
                    val action = { if (operation == "manage") validator.validateManageable(application, visit, actor, now)
                        else validator.validateResultRegistrable(application, visit, actor, now) }
                    when {
                        scenario == "valid" || operation == "result" && scenario == "boundary" -> action()
                        scenario.endsWith("owner") -> reject("ASSIGNED_TO_ANOTHER_WORKER", action)
                        else -> reject(if (operation == "manage") "NOT_MODIFIABLE" else "RESULT_NOT_REGISTRABLE", action)
                    }
                }
            }
        }
}
