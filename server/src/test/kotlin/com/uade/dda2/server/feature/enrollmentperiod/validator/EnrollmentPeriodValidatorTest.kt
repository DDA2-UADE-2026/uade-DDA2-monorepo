package com.uade.dda2.server.feature.enrollmentperiod.validator

import com.uade.dda2.server.error.ApiException
import com.uade.dda2.server.feature.auth.entity.User
import com.uade.dda2.server.feature.enrollmentperiod.dto.request.CreateEnrollmentPeriodRequest
import com.uade.dda2.server.feature.enrollmentperiod.dto.request.UpdateEnrollmentPeriodRequest
import com.uade.dda2.server.feature.enrollmentperiod.entity.EnrollmentPeriod
import com.uade.dda2.server.feature.enrollmentperiod.entity.EnrollmentPeriodStatus
import com.uade.dda2.server.feature.enrollmentperiod.repository.EnrollmentPeriodRepository
import com.uade.dda2.server.feature.program.entity.Program
import com.uade.dda2.server.feature.program.entity.ProgramEdition
import com.uade.dda2.server.feature.program.entity.enums.ProgramEditionStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EnrollmentPeriodValidatorTest {

    private lateinit var repository: EnrollmentPeriodRepository
    private lateinit var validator: EnrollmentPeriodValidator

    @BeforeEach
    fun setUp() {
        repository = mock(EnrollmentPeriodRepository::class.java)
        validator = EnrollmentPeriodValidator(repository)
    }

    @Test
    fun `rechaza un rango con fecha de cierre anterior a la apertura`() {
        val exception = assertFailsWith<ApiException> {
            validator.validateCreate(
                programEdition = edition(),
                request = CreateEnrollmentPeriodRequest(
                    openDate = LocalDate.of(2026, 6, 20),
                    closeDate = LocalDate.of(2026, 6, 19),
                    notes = null,
                ),
            )
        }

        assertEquals("ENROLLMENT_PERIOD_INVALID_DATE_RANGE", exception.code)
    }

    @Test
    fun `rechaza un período fuera de las fechas de la edición`() {
        val exception = assertFailsWith<ApiException> {
            validator.validateCreate(
                programEdition = edition(),
                request = CreateEnrollmentPeriodRequest(
                    openDate = LocalDate.of(2026, 5, 31),
                    closeDate = LocalDate.of(2026, 6, 15),
                    notes = null,
                ),
            )
        }

        assertEquals("ENROLLMENT_PERIOD_OUTSIDE_EDITION_DATE_RANGE", exception.code)
    }

    @Test
    fun `considera superposición cuando dos períodos comparten un día límite`() {
        val edition = edition()
        `when`(
            repository.existsOverlapping(
                programEditionId = requireNotNull(edition.id),
                openDate = LocalDate.of(2026, 6, 15),
                closeDate = LocalDate.of(2026, 6, 20),
            ),
        ).thenReturn(true)

        val exception = assertFailsWith<ApiException> {
            validator.validateCreate(
                programEdition = edition,
                request = CreateEnrollmentPeriodRequest(
                    openDate = LocalDate.of(2026, 6, 15),
                    closeDate = LocalDate.of(2026, 6, 20),
                    notes = null,
                ),
            )
        }

        assertEquals("ENROLLMENT_PERIOD_OVERLAPS_EXISTING_PERIOD", exception.code)
    }

    @Test
    fun `al editar excluye el período actual de la búsqueda de solapamientos`() {
        val enrollmentPeriod = enrollmentPeriod()
        val request = UpdateEnrollmentPeriodRequest(
            openDate = LocalDate.of(2026, 6, 10),
            closeDate = LocalDate.of(2026, 6, 25),
            notes = "Actualizado",
        )
        `when`(
            repository.existsOverlappingExcluding(
                programEditionId = requireNotNull(enrollmentPeriod.programEdition.id),
                excludedId = requireNotNull(enrollmentPeriod.id),
                openDate = request.openDate,
                closeDate = request.closeDate,
            ),
        ).thenReturn(false)

        validator.validateUpdate(
            enrollmentPeriod = enrollmentPeriod,
            request = request,
        )
    }

    @Test
    fun `permite abrir en los límites inclusivos del período`() {
        val enrollmentPeriod = enrollmentPeriod(
            status = EnrollmentPeriodStatus.SCHEDULED,
        )

        validator.validateOpen(
            enrollmentPeriod = enrollmentPeriod,
            currentDate = enrollmentPeriod.openDate,
        )
        validator.validateOpen(
            enrollmentPeriod = enrollmentPeriod,
            currentDate = enrollmentPeriod.closeDate,
        )
    }

    @Test
    fun `no permite abrir anticipadamente`() {
        val enrollmentPeriod = enrollmentPeriod(
            status = EnrollmentPeriodStatus.SCHEDULED,
        )

        val exception = assertFailsWith<ApiException> {
            validator.validateOpen(
                enrollmentPeriod = enrollmentPeriod,
                currentDate = enrollmentPeriod.openDate.minusDays(1),
            )
        }

        assertEquals("ENROLLMENT_PERIOD_OUTSIDE_ACTIVE_DATE_RANGE", exception.code)
    }

    @Test
    fun `solo permite abrir si la edición está activa`() {
        val enrollmentPeriod = enrollmentPeriod(
            status = EnrollmentPeriodStatus.SCHEDULED,
            editionStatus = ProgramEditionStatus.SUSPENDED,
        )

        val exception = assertFailsWith<ApiException> {
            validator.validateOpen(
                enrollmentPeriod = enrollmentPeriod,
                currentDate = enrollmentPeriod.openDate,
            )
        }

        assertEquals("ENROLLMENT_PERIOD_EDITION_NOT_ACTIVE", exception.code)
    }

    @Test
    fun `no permite abrir si la edición ya tiene otro período abierto`() {
        val enrollmentPeriod = enrollmentPeriod(
            status = EnrollmentPeriodStatus.SCHEDULED,
        )
        `when`(
            repository.existsByProgramEditionIdAndStatusAndIdNot(
                programEditionId = requireNotNull(enrollmentPeriod.programEdition.id),
                status = EnrollmentPeriodStatus.OPEN,
                id = requireNotNull(enrollmentPeriod.id),
            ),
        ).thenReturn(true)

        val exception = assertFailsWith<ApiException> {
            validator.validateOpen(
                enrollmentPeriod = enrollmentPeriod,
                currentDate = enrollmentPeriod.openDate,
            )
        }

        assertEquals("ENROLLMENT_PERIOD_ALREADY_OPEN_FOR_EDITION", exception.code)
    }

    @Test
    fun `no permite reabrir después de la fecha de cierre`() {
        val enrollmentPeriod = enrollmentPeriod(
            status = EnrollmentPeriodStatus.SUSPENDED,
        )

        val exception = assertFailsWith<ApiException> {
            validator.validateReopen(
                enrollmentPeriod = enrollmentPeriod,
                currentDate = enrollmentPeriod.closeDate.plusDays(1),
            )
        }

        assertEquals("ENROLLMENT_PERIOD_CANNOT_REOPEN_AFTER_CLOSE_DATE", exception.code)
    }

    @Test
    fun `solo un período abierto puede suspenderse`() {
        validator.validateSuspend(
            enrollmentPeriod(status = EnrollmentPeriodStatus.OPEN),
        )

        val exception = assertFailsWith<ApiException> {
            validator.validateSuspend(
                enrollmentPeriod(status = EnrollmentPeriodStatus.SCHEDULED),
            )
        }

        assertEquals("ENROLLMENT_PERIOD_INVALID_STATUS_TRANSITION", exception.code)
    }

    @Test
    fun `solo períodos abiertos o suspendidos pueden cerrarse`() {
        validator.validateClose(
            enrollmentPeriod(status = EnrollmentPeriodStatus.OPEN),
        )
        validator.validateClose(
            enrollmentPeriod(status = EnrollmentPeriodStatus.SUSPENDED),
        )

        val exception = assertFailsWith<ApiException> {
            validator.validateClose(
                enrollmentPeriod(status = EnrollmentPeriodStatus.SCHEDULED),
            )
        }

        assertEquals("ENROLLMENT_PERIOD_INVALID_STATUS_TRANSITION", exception.code)
    }

    @Test
    fun `solo acepta solicitudes de un período abierto vigente y de la misma edición`() {
        val enrollmentPeriod = enrollmentPeriod(
            status = EnrollmentPeriodStatus.OPEN,
        )

        validator.validateCanReceiveApplication(
            programEditionId = requireNotNull(enrollmentPeriod.programEdition.id),
            enrollmentPeriod = enrollmentPeriod,
            currentDate = enrollmentPeriod.openDate,
        )

        enrollmentPeriod.status = EnrollmentPeriodStatus.SUSPENDED
        val exception = assertFailsWith<ApiException> {
            validator.validateCanReceiveApplication(
                programEditionId = requireNotNull(enrollmentPeriod.programEdition.id),
                enrollmentPeriod = enrollmentPeriod,
                currentDate = enrollmentPeriod.openDate,
            )
        }

        assertEquals("ENROLLMENT_PERIOD_NOT_OPEN", exception.code)
    }

    private fun enrollmentPeriod(
        status: EnrollmentPeriodStatus = EnrollmentPeriodStatus.SCHEDULED,
        editionStatus: ProgramEditionStatus = ProgramEditionStatus.ACTIVE,
    ): EnrollmentPeriod =
        EnrollmentPeriod(
            id = UUID.randomUUID(),
            programEdition = edition(status = editionStatus),
            openDate = LocalDate.of(2026, 6, 10),
            closeDate = LocalDate.of(2026, 6, 25),
            status = status,
            notes = null,
        )

    private fun edition(
        status: ProgramEditionStatus = ProgramEditionStatus.ACTIVE,
    ): ProgramEdition {
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

        return ProgramEdition(
            id = UUID.randomUUID(),
            program = program,
            name = "Edición 2026",
            startDate = LocalDate.of(2026, 6, 1),
            endDate = LocalDate.of(2026, 6, 30),
            maxCapacity = 100,
            status = status,
            createdBy = user,
        )
    }
}
