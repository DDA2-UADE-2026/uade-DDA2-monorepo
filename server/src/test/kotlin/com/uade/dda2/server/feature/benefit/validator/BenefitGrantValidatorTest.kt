package com.uade.dda2.server.feature.benefit.validator

import com.uade.dda2.server.error.BadRequestException
import com.uade.dda2.server.feature.auth.entity.User
import com.uade.dda2.server.feature.benefit.dto.request.CreateBenefitGrantRequest
import com.uade.dda2.server.feature.program.entity.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID
import kotlin.test.*

class BenefitGrantValidatorTest {
    private val validator = BenefitGrantValidator()
    private val today = LocalDate.of(2026, 10, 19)
    private val user = User(id = 1)
    private val edition = ProgramEdition(program = Program(name = "Programa", createdBy = user), name = "Edicion",
        startDate = today.minusDays(10), endDate = today.plusDays(10), maxCapacity = 1, createdBy = user)
    private val request = CreateBenefitGrantRequest(UUID.randomUUID(), today.minusDays(1), today.plusDays(1))

    @Test fun `requiere identificador y ambas fechas`() {
        listOf(request.copy(programBenefitId = null), request.copy(startDate = null), request.copy(endDate = null)).forEach {
            assertFailsWith<BadRequestException> { validator.validateRequest(it, edition, today) }
        }
    }
    @Test fun `permite extremos inclusivos y vigencia de un dia`() {
        assertEquals(today, validator.validateRequest(request.copy(startDate = today, endDate = today), edition, today).startDate)
        val data = validator.validateRequest(request.copy(startDate = edition.startDate, endDate = edition.endDate), edition, today)
        assertEquals(edition.startDate, data.startDate)
        assertEquals(edition.endDate, data.endDate)
    }
    @Test fun `rechaza fechas fuera de edicion invertidas futuras o vencidas`() {
        listOf(request.copy(startDate = today.plusDays(2)), request.copy(endDate = today.minusDays(2)),
            request.copy(startDate = today.minusDays(11)), request.copy(endDate = today.plusDays(11))).forEach {
            assertFailsWith<BadRequestException> { validator.validateRequest(it, edition, today) }
        }
    }
    @Test fun `usa el dia recibido del servidor y no la fecha del sistema`() {
        assertFailsWith<BadRequestException> { validator.validateRequest(request, edition, today.plusDays(3)) }
        validator.validateRequest(request, edition, today)
    }
}
