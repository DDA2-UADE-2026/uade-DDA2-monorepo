package com.uade.dda2.server.feature.application.validator

import com.uade.dda2.server.error.ApiException
import com.uade.dda2.server.feature.application.dto.request.*
import com.uade.dda2.server.feature.application.entity.ApplicationVisitRescheduleDecision
import com.uade.dda2.server.feature.application.entity.ApplicationVisitRescheduleRequestStatus
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.DynamicTest.dynamicTest
import java.time.OffsetDateTime
import kotlin.test.*

class VisitRescheduleBranchTest {
    private val validator = ApplicationVisitRescheduleRequestValidator()
    private fun reject(action: () -> Unit) = assertEquals("APPLICATION_VISIT_RESCHEDULE_REQUEST_INVALID_DATA",
        assertFailsWith<ApiException>(block = action).code)

    @TestFactory
    fun `reason must be nonempty after trimming and at most 1000 characters`() =
        listOf(null, "", " ", "a".repeat(1001), " x ", "a".repeat(1000)).map { value -> dynamicTest("reason length=${value?.length}") {
            val request = CreateApplicationVisitRescheduleRequest(value)
            if (value.isNullOrBlank() || value.trim().length > 1000) reject { validator.validateCreate(request) }
            else assertEquals(value.trim(), validator.validateCreate(request))
        } }

    @TestFactory
    fun `review comments are optional for approval and mandatory for rejection`() =
        ApplicationVisitRescheduleDecision.entries.flatMap { decision ->
            listOf(null, "", " ", "a".repeat(1001), " explanation ", "a".repeat(1000)).map { comment -> dynamicTest("$decision comment length=${comment?.length}") {
                val request = ReviewApplicationVisitRescheduleRequest(status = decision, comment = comment)
                val valid = if (comment == null) decision == ApplicationVisitRescheduleDecision.APPROVED
                    else comment.trim().isNotEmpty() && comment.trim().length <= 1000
                if (!valid) reject { validator.validateReview(request) }
                else {
                    val result = validator.validateReview(request)
                    assertEquals(comment?.trim(), result.comment)
                    assertEquals(ApplicationVisitRescheduleRequestStatus.valueOf(decision.name), result.status)
                }
            } }
        }

    @Test
    fun `rejection cannot carry new scheduling fields and decision is mandatory`() {
        reject { validator.validateReview(ReviewApplicationVisitRescheduleRequest()) }
        val base = ReviewApplicationVisitRescheduleRequest(status = ApplicationVisitRescheduleDecision.REJECTED, comment = "reason")
        listOf(base.copy(startsAt = OffsetDateTime.parse("2026-11-15T12:00:00Z")), base.copy(durationMinutes = 30), base.copy(location = "office"))
            .forEach { request -> reject { validator.validateReview(request) } }
    }
}
