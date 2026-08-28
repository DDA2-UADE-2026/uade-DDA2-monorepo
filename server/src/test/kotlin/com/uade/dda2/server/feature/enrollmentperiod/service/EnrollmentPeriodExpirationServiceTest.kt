package com.uade.dda2.server.feature.enrollmentperiod.service

import com.uade.dda2.server.feature.auth.entity.User
import com.uade.dda2.server.feature.enrollmentperiod.entity.EnrollmentPeriod
import com.uade.dda2.server.feature.enrollmentperiod.entity.EnrollmentPeriodStatus
import com.uade.dda2.server.feature.enrollmentperiod.repository.EnrollmentPeriodRepository
import com.uade.dda2.server.feature.log.entity.LogAction
import com.uade.dda2.server.feature.log.entity.LogEntityType
import com.uade.dda2.server.feature.log.service.LogService
import com.uade.dda2.server.feature.program.entity.Program
import com.uade.dda2.server.feature.program.entity.ProgramEdition
import com.uade.dda2.server.feature.program.entity.enums.ProgramEditionStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.ArgumentMatchers.isNull
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import tools.jackson.databind.json.JsonMapper
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals

class EnrollmentPeriodExpirationServiceTest {

    private lateinit var repository: EnrollmentPeriodRepository
    private lateinit var logService: LogService
    private lateinit var jsonMapper: JsonMapper
    private lateinit var service: EnrollmentPeriodExpirationService

    @BeforeEach
    fun setUp() {
        repository = mock(EnrollmentPeriodRepository::class.java)
        logService = mock(LogService::class.java)
        jsonMapper = mock(JsonMapper::class.java)
        service = EnrollmentPeriodExpirationService(
            enrollmentPeriodRepository = repository,
            logService = logService,
            jsonMapper = jsonMapper,
        )

        `when`(jsonMapper.writeValueAsString(any())).thenReturn("{}")
    }

    @Test
    fun `cierra períodos vencidos una sola vez y los audita como sistema`() {
        val currentDate = LocalDate.of(2026, 9, 1)
        val expiredPeriods = listOf(
            enrollmentPeriod(EnrollmentPeriodStatus.SCHEDULED),
            enrollmentPeriod(EnrollmentPeriodStatus.OPEN),
            enrollmentPeriod(EnrollmentPeriodStatus.SUSPENDED),
        )
        `when`(
            repository.findExpiredForUpdate(
                statuses = setOf(
                    EnrollmentPeriodStatus.SCHEDULED,
                    EnrollmentPeriodStatus.OPEN,
                    EnrollmentPeriodStatus.SUSPENDED,
                ),
                currentDate = currentDate,
            ),
        ).thenReturn(expiredPeriods, emptyList())

        assertEquals(3, service.closeExpired(currentDate))
        assertEquals(0, service.closeExpired(currentDate))
        expiredPeriods.forEach { enrollmentPeriod ->
            assertEquals(EnrollmentPeriodStatus.CLOSED, enrollmentPeriod.status)
        }

        verify(logService, times(3)).record(
            user = isNull(),
            action = eq(LogAction.UPDATE),
            entityType = eq(LogEntityType.ENROLLMENT_PERIOD),
            entityId = anyString(),
            oldValues = anyString(),
            newValues = anyString(),
        )
    }

    private fun enrollmentPeriod(status: EnrollmentPeriodStatus): EnrollmentPeriod {
        val user = User(
            id = 1L,
            username = "admin",
            passwordHash = "hash",
            name = "Admin",
            email = "admin@example.com",
        )
        val program = Program(
            id = UUID.randomUUID(),
            name = "Programa",
            createdBy = user,
        )
        val edition = ProgramEdition(
            id = UUID.randomUUID(),
            program = program,
            name = "Edición 2026",
            startDate = LocalDate.of(2026, 8, 1),
            endDate = LocalDate.of(2026, 12, 31),
            maxCapacity = 100,
            status = ProgramEditionStatus.ACTIVE,
            createdBy = user,
        )

        return EnrollmentPeriod(
            id = UUID.randomUUID(),
            programEdition = edition,
            openDate = LocalDate.of(2026, 8, 1),
            closeDate = LocalDate.of(2026, 8, 31),
            status = status,
        )
    }
}
