package com.uade.dda2.server.feature.activity.validator

import com.uade.dda2.server.error.ApiException
import com.uade.dda2.server.feature.activity.dto.request.CreateActivityRequest
import com.uade.dda2.server.feature.activity.dto.request.UpdateActivityRequest
import com.uade.dda2.server.feature.activity.entity.Activity
import com.uade.dda2.server.feature.activity.entity.ActivityStatus
import com.uade.dda2.server.feature.auth.entity.User
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ActivityValidatorTest {
    private val validator = ActivityValidator()

    @Test
    fun `acepta actividades de un solo dia`() {
        validator.validateCreate(createRequest())
    }

    @Test
    fun `rechaza fecha final anterior a la inicial`() {
        val exception = assertFailsWith<ApiException> {
            validator.validateCreate(
                createRequest(
                    startDate = LocalDate.of(2026, 10, 11),
                    endDate = LocalDate.of(2026, 10, 10),
                ),
            )
        }

        assertEquals("ACTIVITY_INVALID_DATE_RANGE", exception.code)
    }

    @Test
    fun `solo permite editar borradores`() {
        validator.validateUpdate(activity(ActivityStatus.DRAFT), updateRequest())

        for (status in listOf(ActivityStatus.OPEN, ActivityStatus.CLOSED)) {
            val exception = assertFailsWith<ApiException> {
                validator.validateUpdate(activity(status), updateRequest())
            }
            assertEquals("ACTIVITY_CANNOT_BE_EDITED", exception.code)
        }
    }

    @Test
    fun `solo permite publicar borradores`() {
        validator.validatePublish(activity(ActivityStatus.DRAFT))

        for (status in listOf(ActivityStatus.OPEN, ActivityStatus.CLOSED)) {
            val exception = assertFailsWith<ApiException> {
                validator.validatePublish(activity(status))
            }
            assertEquals("ACTIVITY_INVALID_STATUS_TRANSITION", exception.code)
        }
    }

    @Test
    fun `solo permite cerrar actividades abiertas`() {
        validator.validateClose(activity(ActivityStatus.OPEN))

        for (status in listOf(ActivityStatus.DRAFT, ActivityStatus.CLOSED)) {
            val exception = assertFailsWith<ApiException> {
                validator.validateClose(activity(status))
            }
            assertEquals("ACTIVITY_INVALID_STATUS_TRANSITION", exception.code)
        }
    }

    private fun createRequest(
        startDate: LocalDate = LocalDate.of(2026, 10, 10),
        endDate: LocalDate = startDate,
    ) = CreateActivityRequest(
        name = "Taller comunitario de RCP",
        description = "Capacitación abierta.",
        location = "Centro Municipal Norte",
        startDate = startDate,
        endDate = endDate,
        capacity = 30,
    )

    private fun updateRequest() = UpdateActivityRequest(
        name = "Taller actualizado",
        description = "Descripción actualizada.",
        location = "Centro Municipal Sur",
        startDate = LocalDate.of(2026, 10, 10),
        endDate = LocalDate.of(2026, 10, 11),
        capacity = 40,
    )

    private fun activity(status: ActivityStatus) = Activity(
        name = "Actividad",
        description = "Descripción",
        location = "Centro",
        startDate = LocalDate.of(2026, 10, 10),
        endDate = LocalDate.of(2026, 10, 10),
        capacity = 10,
        status = status,
        createdBy = User(name = "Admin", email = "admin@example.com"),
    )
}
