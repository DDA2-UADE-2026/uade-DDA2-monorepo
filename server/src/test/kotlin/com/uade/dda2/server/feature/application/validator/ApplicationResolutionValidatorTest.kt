package com.uade.dda2.server.feature.application.validator

import com.uade.dda2.server.error.BadRequestException
import com.uade.dda2.server.error.ConflictException
import com.uade.dda2.server.feature.application.dto.request.*
import com.uade.dda2.server.feature.application.entity.ApplicationResolutionDecision
import com.uade.dda2.server.feature.auth.entity.User
import com.uade.dda2.server.feature.program.entity.*
import com.uade.dda2.server.feature.program.entity.enums.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID
import kotlin.test.*

class ApplicationResolutionValidatorTest {
    private val validator = ApplicationResolutionValidator()
    private val user = User(id = 1, name = "Coordinador")
    private val edition = ProgramEdition(id = UUID.randomUUID(), program = Program(id = UUID.randomUUID(), name = "Programa", createdBy = user),
        name = "Edicion", startDate = LocalDate.now().minusDays(1), endDate = LocalDate.now(), maxCapacity = 1,
        status = ProgramEditionStatus.ACTIVE, createdBy = user)
    private val requirement = ProgramRequirement(id = UUID.randomUUID(), programEdition = edition,
        type = ProgramRequirementType.HAS_CHILDREN, value = "true")

    @Test fun `normaliza motivo preserva resultado negativo y exige campos explicitos`() {
        val request = CreateApplicationResolutionRequest(ApplicationResolutionDecision.REJECTED, " Motivo ",
            listOf(ApplicationRequirementCheckRequest(requirement.id, false)))
        val data = validator.validateRequest(request, listOf(requirement))
        assertEquals("Motivo", data.reason)
        assertEquals(false, data.requirementChecks[requirement.id])
        listOf(request.copy(decision = null), request.copy(reason = null), request.copy(requirementChecks = null),
            request.copy(requirementChecks = listOf(null)), request.copy(requirementChecks = listOf(ApplicationRequirementCheckRequest(requirement.id, null))))
            .forEach { assertFailsWith<BadRequestException> { validator.validateRequest(it, listOf(requirement)) } }
    }

    @Test fun `requiere todos los controles sin duplicados ni ajenos`() {
        val request = CreateApplicationResolutionRequest(ApplicationResolutionDecision.APPROVED, "Motivo", emptyList())
        val check = ApplicationRequirementCheckRequest(requirement.id, true)
        listOf(emptyList(), listOf(check, check), listOf(ApplicationRequirementCheckRequest(UUID.randomUUID(), true)))
            .forEach { assertFailsWith<BadRequestException> { validator.validateRequest(request.copy(requirementChecks = it), listOf(requirement)) } }
        assertTrue(validator.validateRequest(request, emptyList()).requirementChecks.isEmpty())
    }

    @Test fun `limite de motivo se aplica despues de recortar`() {
        val request = CreateApplicationResolutionRequest(ApplicationResolutionDecision.APPROVED, " " + "a".repeat(1000) + " ", emptyList())
        assertEquals(1000, validator.validateRequest(request, emptyList()).reason.length)
        assertFailsWith<BadRequestException> { validator.validateRequest(request.copy(reason = "a".repeat(1001)), emptyList()) }
    }

    @Test fun `aprobar y esperar tienen condiciones de cupo opuestas`() {
        val data = ValidatedApplicationResolution(ApplicationResolutionDecision.APPROVED, "Motivo", emptyMap())
        validator.validateEligibility(data, edition, LocalDate.now(), true, false)
        assertFailsWith<ConflictException> { validator.validateEligibility(data.copy(decision = ApplicationResolutionDecision.WAITLISTED), edition, LocalDate.now(), true, false) }
        edition.currentEnrollment = 1
        assertFailsWith<ConflictException> { validator.validateEligibility(data, edition, LocalDate.now(), true, false) }
        validator.validateEligibility(data.copy(decision = ApplicationResolutionDecision.WAITLISTED), edition, LocalDate.now(), true, false)
    }

    @Test fun `rechazo no exige elegibilidad adicional`() {
        edition.status = ProgramEditionStatus.CLOSED
        validator.validateEligibility(ValidatedApplicationResolution(ApplicationResolutionDecision.REJECTED, "Motivo", mapOf(UUID.randomUUID() to false)),
            edition, LocalDate.now().plusDays(1), documentsValid = false, incompatible = true)
    }
}
