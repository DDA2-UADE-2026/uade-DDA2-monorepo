package com.uade.dda2.server.feature.program.validator

import com.uade.dda2.server.error.ApiException
import com.uade.dda2.server.feature.auth.entity.User
import com.uade.dda2.server.feature.program.dto.admin.request.*
import com.uade.dda2.server.feature.program.entity.Program
import com.uade.dda2.server.feature.program.entity.enums.ProgramBenefitType
import com.uade.dda2.server.feature.program.repository.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import java.math.BigDecimal
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ProgramCatalogBranchTest {
    private fun reject(code: String, action: () -> Unit) =
        assertEquals(code, assertFailsWith<ApiException>(block = action).code)

    @Test
    fun `program names are normalized and update excludes its own id`() {
        val programs = mock(ProgramRepository::class.java)
        val validator = AdminProgramValidator(programs, mock(ProgramEditionRepository::class.java), mock(ProgramIncompatibilityRepository::class.java))
        val program = Program(id = UUID.randomUUID(), name = "Programa", createdBy = User())
        validator.validateCreate(CreateProgramRequest("  PROGRAMA  "))
        verify(programs).existsByNormalizedName("programa")
        validator.validateUpdate(program, UpdateProgramRequest("  PROGRAMA  "))
        verify(programs).existsByNormalizedNameAndIdNot("programa", program.id!!)
        `when`(programs.existsByNormalizedName("programa")).thenReturn(true)
        reject("PROGRAM_NAME_ALREADY_EXISTS") { validator.validateCreate(CreateProgramRequest("PROGRAMA")) }
        `when`(programs.existsByNormalizedNameAndIdNot("programa", program.id!!)).thenReturn(true)
        reject("PROGRAM_NAME_ALREADY_EXISTS") { validator.validateUpdate(program, UpdateProgramRequest("PROGRAMA")) }
    }

    @Test
    fun `program deletion is blocked by editions or incompatibilities independently`() {
        val editions = mock(ProgramEditionRepository::class.java)
        val incompatibilities = mock(ProgramIncompatibilityRepository::class.java)
        val validator = AdminProgramValidator(mock(ProgramRepository::class.java), editions, incompatibilities)
        val program = Program(id = UUID.randomUUID(), name = "Programa", createdBy = User())
        validator.validateDelete(program)
        `when`(editions.existsByProgramId(program.id!!)).thenReturn(true)
        reject("PROGRAM_HAS_EDITIONS") { validator.validateDelete(program) }
        `when`(editions.existsByProgramId(program.id!!)).thenReturn(false)
        `when`(incompatibilities.existsByProgramId(program.id!!)).thenReturn(true)
        reject("PROGRAM_HAS_INCOMPATIBILITIES") { validator.validateDelete(program) }
    }

    @Test
    fun `incompatibility requires two different programs and a new relationship`() {
        val repository = mock(ProgramIncompatibilityRepository::class.java)
        val validator = AdminProgramIncompatibilityValidator(repository)
        val first = Program(id = UUID.randomUUID(), name = "First", createdBy = User())
        val second = Program(id = UUID.randomUUID(), name = "Second", createdBy = User())
        reject("PROGRAM_INCOMPATIBILITY_SAME_PROGRAM") { validator.validateCreate(first, first) }
        verifyNoInteractions(repository)
        validator.validateCreate(first, second)
        verify(repository).existsBetweenPrograms(first.id!!, second.id!!)
        `when`(repository.existsBetweenPrograms(first.id!!, second.id!!)).thenReturn(true)
        reject("PROGRAM_INCOMPATIBILITY_ALREADY_EXISTS") { validator.validateCreate(first, second) }
    }

    @Test
    fun `benefits accept absent zero or positive amount but reject negative amounts`() {
        val validator = AdminProgramBenefitValidator()
        val type = ProgramBenefitType.entries.first()
        listOf(null, BigDecimal.ZERO, BigDecimal("0.01")).forEach { amount ->
            validator.validateCreate(CreateProgramBenefitRequest(type, amount = amount))
            validator.validateUpdate(UpdateProgramBenefitRequest(type, amount = amount))
        }
        reject("PROGRAM_BENEFIT_INVALID_AMOUNT") { validator.validateCreate(CreateProgramBenefitRequest(type, amount = BigDecimal("-0.01"))) }
        reject("PROGRAM_BENEFIT_INVALID_AMOUNT") { validator.validateUpdate(UpdateProgramBenefitRequest(type, amount = BigDecimal("-0.01"))) }
    }
}
