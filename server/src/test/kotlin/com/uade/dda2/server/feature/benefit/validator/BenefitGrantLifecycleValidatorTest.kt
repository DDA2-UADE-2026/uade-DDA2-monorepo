package com.uade.dda2.server.feature.benefit.validator

import com.uade.dda2.server.error.BadRequestException
import com.uade.dda2.server.error.ConflictException
import com.uade.dda2.server.feature.application.entity.*
import com.uade.dda2.server.feature.auth.entity.User
import com.uade.dda2.server.feature.benefit.dto.request.*
import com.uade.dda2.server.feature.benefit.entity.*
import com.uade.dda2.server.feature.enrollmentperiod.entity.EnrollmentPeriod
import com.uade.dda2.server.feature.program.entity.*
import com.uade.dda2.server.feature.program.entity.enums.*
import org.junit.jupiter.api.Test
import java.time.*
import java.util.UUID
import kotlin.test.*

class BenefitGrantLifecycleValidatorTest {
    private val validator = BenefitGrantLifecycleValidator()
    private val today = LocalDate.of(2026, 10, 19)
    private val user = User(id = 1)
    private val edition = ProgramEdition(program = Program(name = "Programa", createdBy = user), name = "Edicion",
        startDate = today.minusDays(20), endDate = today.plusDays(20), maxCapacity = 1, status = ProgramEditionStatus.ACTIVE, createdBy = user)
    private val application = Application(user = user, registeredBy = user, programEdition = edition,
        enrollmentPeriod = EnrollmentPeriod(programEdition = edition, openDate = today, closeDate = today),
        status = ApplicationStatus.APPROVED, submittedAt = today.atStartOfDay())
    private val grant = BenefitGrant(application = application, programBenefit = ProgramBenefit(programEdition = edition,
        benefitType = ProgramBenefitType.FOOD_ASSISTANCE), benefitType = ProgramBenefitType.FOOD_ASSISTANCE, description = null,
        amount = null, startDate = today.minusDays(5), endDate = today.plusDays(5), assignedBy = user, assignedAt = OffsetDateTime.now(ZoneOffset.UTC))
    private val request = RenewBenefitGrantRequest(" Motivo ", today.plusDays(10), emptyList())

    @Test fun `motivo recortado admite limites y rechaza vacios nulos y extensos`() {
        assertEquals("Motivo", validator.reason(" Motivo "))
        assertEquals(1000, validator.reason("x".repeat(1000)).length)
        listOf(null, "", " \t\n ", "x".repeat(1001)).forEach {
            assertFailsWith<BadRequestException> { validator.reason(it) }
        }
    }
    @Test fun `renovacion requiere fecha y checklist explicitos`() {
        listOf(request.copy(endDate = null), request.copy(requirementChecks = null),
            request.copy(requirementChecks = listOf(null)),
            request.copy(requirementChecks = listOf(BenefitGrantRequirementCheckRequest()))).forEach {
            assertFailsWith<BadRequestException> { validator.validateRenewal(it, grant, edition, today, emptyList()) }
        }
    }
    @Test fun `checklist distingue estructura invalida de requisito incumplido`() {
        val requirement = ProgramRequirement(id = UUID.randomUUID(), programEdition = edition,
            type = ProgramRequirementType.MIN_AGE, value = "18")
        val check = BenefitGrantRequirementCheckRequest(requirement.id, true)
        listOf(emptyList(), listOf(check, check), listOf(check.copy(requirementId = UUID.randomUUID())),
            listOf(check.copy(fulfilled = null))).forEach {
            assertFailsWith<BadRequestException> { validator.validateRenewal(request.copy(requirementChecks = it), grant, edition, today, listOf(requirement)) }
        }
        assertFailsWith<ConflictException> {
            validator.validateRenewal(request.copy(requirementChecks = listOf(check.copy(fulfilled = false))), grant, edition, today, listOf(requirement))
        }
        val result = validator.validateRenewal(request.copy(requirementChecks = listOf(check)), grant, edition, today, listOf(requirement))
        assertEquals(mapOf(requireNotNull(requirement.id) to true), result.checks)
    }
    @Test fun `acepta extension inclusiva y usa hoy recibido incluso si otorgamiento vencio`() {
        grant.endDate = today.minusDays(1)
        assertEquals(today, validator.validateRenewal(request.copy(endDate = today), grant, edition, today, emptyList()).endDate)
        assertEquals(edition.endDate, validator.validateRenewal(request.copy(endDate = edition.endDate), grant, edition, today, emptyList()).endDate)
        assertFailsWith<BadRequestException> { validator.validateRenewal(request.copy(endDate = today), grant, edition, today.plusDays(1), emptyList()) }
        assertFailsWith<BadRequestException> { validator.validateRenewal(request.copy(endDate = today.minusDays(1)), grant, edition, today, emptyList()) }
        assertFailsWith<BadRequestException> { validator.validateRenewal(request.copy(endDate = edition.endDate.plusDays(1)), grant, edition, today, emptyList()) }
        assertFailsWith<BadRequestException> { validator.validateRenewal(request, grant, edition, grant.startDate.minusDays(1), emptyList()) }
    }
    @Test fun `transiciones son definitivas y no aceptan asignacion como cambio`() {
        validator.validateStage(grant, BenefitGrantChangeAction.RENEWED)
        validator.validateStage(grant, BenefitGrantChangeAction.SUSPENDED)
        validator.validateStage(grant, BenefitGrantChangeAction.FINALIZED)
        grant.status = BenefitGrantStatus.SUSPENDED
        validator.validateStage(grant, BenefitGrantChangeAction.RENEWED)
        validator.validateStage(grant, BenefitGrantChangeAction.FINALIZED)
        assertFailsWith<ConflictException> { validator.validateStage(grant, BenefitGrantChangeAction.SUSPENDED) }
        assertFailsWith<ConflictException> { validator.validateStage(grant, BenefitGrantChangeAction.ASSIGNED) }
        grant.status = BenefitGrantStatus.FINALIZED
        BenefitGrantChangeAction.entries.forEach { assertFailsWith<ConflictException> { validator.validateStage(grant, it) } }
        grant.status = BenefitGrantStatus.ACTIVE
        application.status = ApplicationStatus.CLOSED
        assertFailsWith<ConflictException> { validator.validateStage(grant, BenefitGrantChangeAction.RENEWED) }
    }
}
