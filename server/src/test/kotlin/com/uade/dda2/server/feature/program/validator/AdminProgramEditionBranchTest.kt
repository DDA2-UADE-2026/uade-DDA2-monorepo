package com.uade.dda2.server.feature.program.validator

import com.uade.dda2.server.error.ApiException
import com.uade.dda2.server.feature.auth.entity.User
import com.uade.dda2.server.feature.benefit.repository.BenefitGrantRepository
import com.uade.dda2.server.feature.enrollmentperiod.entity.EnrollmentPeriodStatus
import com.uade.dda2.server.feature.enrollmentperiod.repository.EnrollmentPeriodRepository
import com.uade.dda2.server.feature.program.dto.admin.request.*
import com.uade.dda2.server.feature.program.entity.*
import com.uade.dda2.server.feature.program.entity.enums.ProgramEditionStatus
import com.uade.dda2.server.feature.program.repository.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import org.mockito.Mockito.*
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AdminProgramEditionBranchTest {
    private class Fixture {
        val editions = mock(ProgramEditionRepository::class.java)
        val benefits = mock(ProgramBenefitRepository::class.java)
        val requirements = mock(ProgramRequirementRepository::class.java)
        val documents = mock(ProgramDocumentRequirementRepository::class.java)
        val periods = mock(EnrollmentPeriodRepository::class.java)
        val grants = mock(BenefitGrantRepository::class.java)
        val validator = AdminProgramEditionValidator(editions, benefits, requirements, documents, periods, grants)
        val user = User(id = 1, username = "qa")
        val program = Program(id = UUID.randomUUID(), name = "Programa", createdBy = user)
        val date = LocalDate.of(2026, 6, 1)
        val edition = ProgramEdition(id = UUID.randomUUID(), program = program, name = "Edicion",
            startDate = date, endDate = date.plusDays(30), maxCapacity = 10, createdBy = user)
        fun create(capacity: Int = 10, end: LocalDate = date) =
            CreateProgramEditionRequest("  EDICION  ", date, end, capacity)
        fun update(capacity: Int = 10, end: LocalDate = date.plusDays(30)) =
            UpdateProgramEditionRequest("  EDICION  ", date, end, capacity)
    }

    private fun rejects(suffix: String, action: () -> Unit) {
        assertEquals("PROGRAM_EDITION_$suffix", assertFailsWith<ApiException>(block = action).code)
    }

    @TestFactory
    fun `transition matrix respects the edition lifecycle`() = ProgramEditionStatus.entries.flatMap { from ->
        ProgramEditionStatus.entries.map { to -> dynamicTest("$from -> $to") {
            val f = Fixture()
            f.edition.status = from
            val allowed = when (from) {
                ProgramEditionStatus.DRAFT -> setOf(ProgramEditionStatus.ACTIVE, ProgramEditionStatus.CLOSED)
                ProgramEditionStatus.ACTIVE -> setOf(ProgramEditionStatus.SUSPENDED, ProgramEditionStatus.CLOSED)
                ProgramEditionStatus.SUSPENDED -> setOf(ProgramEditionStatus.ACTIVE, ProgramEditionStatus.CLOSED)
                ProgramEditionStatus.CLOSED -> emptySet()
            }
            if (to in allowed) f.validator.validateStatusTransition(f.edition, to)
            else rejects("INVALID_STATUS_TRANSITION") { f.validator.validateStatusTransition(f.edition, to) }
            assertEquals(from, f.edition.status, "Validation must not mutate the edition")
        } }
    }

    @TestFactory
    fun `open enrollment blocks suspension and closure`() =
        listOf(ProgramEditionStatus.SUSPENDED, ProgramEditionStatus.CLOSED).map { target -> dynamicTest("$target") {
            val f = Fixture()
            f.edition.status = ProgramEditionStatus.ACTIVE
            `when`(f.periods.existsByProgramEditionIdAndStatus(f.edition.id!!, EnrollmentPeriodStatus.OPEN)).thenReturn(true)
            rejects("HAS_OPEN_ENROLLMENT_PERIOD") { f.validator.validateStatusTransition(f.edition, target) }
        } }

    @Test
    fun `creation accepts same day dates and minimum capacity with normalized name`() {
        val f = Fixture()
        f.validator.validateCreate(f.program, f.create(capacity = 1))
        verify(f.editions).existsByProgramIdAndNormalizedName(f.program.id!!, "edicion")
    }

    @Test
    fun `duplicate names are rejected on create and update excluding own id`() {
        val f = Fixture()
        `when`(f.editions.existsByProgramIdAndNormalizedName(f.program.id!!, "edicion")).thenReturn(true)
        rejects("NAME_ALREADY_EXISTS") { f.validator.validateCreate(f.program, f.create()) }
        `when`(f.editions.existsByProgramIdAndNormalizedNameAndIdNot(f.program.id!!, "edicion", f.edition.id!!)).thenReturn(true)
        rejects("NAME_ALREADY_EXISTS") { f.validator.validateUpdate(f.edition, f.update()) }
    }

    @TestFactory
    fun `nonpositive capacity is rejected in both operations`() = listOf(0, -1).map { capacity -> dynamicTest("capacity $capacity") {
        val f = Fixture()
        rejects("INVALID_CAPACITY") { f.validator.validateCreate(f.program, f.create(capacity)) }
        rejects("INVALID_CAPACITY") { f.validator.validateUpdate(f.edition, f.update(capacity)) }
    } }

    @Test
    fun `inverted dates are rejected in both operations`() {
        val f = Fixture()
        rejects("INVALID_DATE_RANGE") { f.validator.validateCreate(f.program, f.create(end = f.date.minusDays(1))) }
        rejects("INVALID_DATE_RANGE") { f.validator.validateUpdate(f.edition, f.update(end = f.date.minusDays(1))) }
    }

    @Test
    fun `capacity can equal enrollment but cannot fall below it`() {
        val f = Fixture()
        f.edition.currentEnrollment = 5
        f.validator.validateUpdate(f.edition, f.update(5))
        verify(f.editions).existsByProgramIdAndNormalizedNameAndIdNot(f.program.id!!, "edicion", f.edition.id!!)
        rejects("CAPACITY_BELOW_ENROLLMENT") { f.validator.validateUpdate(f.edition, f.update(4)) }
    }

    @Test
    fun `date changes preserve enrollment periods and benefit grants`() {
        val f = Fixture()
        val request = f.update()
        `when`(f.periods.existsOutsideDateRange(f.edition.id!!, request.startDate, request.endDate)).thenReturn(true)
        rejects("DATE_RANGE_EXCLUDES_ENROLLMENT_PERIODS") { f.validator.validateUpdate(f.edition, request) }
        `when`(f.periods.existsOutsideDateRange(f.edition.id!!, request.startDate, request.endDate)).thenReturn(false)
        `when`(f.grants.existsOutsideEditionDateRange(f.edition.id!!, request.startDate, request.endDate)).thenReturn(true)
        rejects("DATE_RANGE_EXCLUDES_BENEFIT_GRANTS") { f.validator.validateUpdate(f.edition, request) }
    }

    @TestFactory
    fun `each associated configuration independently prevents deletion`() = (0..3).map { dependency -> dynamicTest("dependency $dependency") {
        val f = Fixture()
        val id = f.edition.id!!
        when (dependency) {
            0 -> `when`(f.benefits.existsByProgramEditionId(id)).thenReturn(true)
            1 -> `when`(f.requirements.existsByProgramEditionId(id)).thenReturn(true)
            2 -> `when`(f.documents.existsByProgramEditionId(id)).thenReturn(true)
            3 -> `when`(f.periods.existsByProgramEditionId(id)).thenReturn(true)
        }
        rejects("HAS_CONFIGURATION") { f.validator.validateDelete(f.edition) }
    } }

    @Test
    fun `deletion requires no enrollment and an edition that is not closed`() {
        val f = Fixture()
        f.validator.validateDelete(f.edition)
        f.edition.currentEnrollment = 1
        rejects("HAS_ENROLLMENTS") { f.validator.validateDelete(f.edition) }
        f.edition.status = ProgramEditionStatus.CLOSED
        rejects("CLOSED_DELETE_NOT_ALLOWED") { f.validator.validateDelete(f.edition) }
        rejects("CLOSED") { f.validator.validateUpdate(f.edition, f.update()) }
    }
}
