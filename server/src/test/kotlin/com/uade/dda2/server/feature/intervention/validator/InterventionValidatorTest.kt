package com.uade.dda2.server.feature.intervention.validator

import com.uade.dda2.server.error.BadRequestException
import com.uade.dda2.server.error.ConflictException
import com.uade.dda2.server.feature.application.entity.*
import com.uade.dda2.server.feature.auth.entity.User
import com.uade.dda2.server.feature.enrollmentperiod.entity.EnrollmentPeriod
import com.uade.dda2.server.feature.intervention.dto.request.*
import com.uade.dda2.server.feature.intervention.entity.*
import com.uade.dda2.server.feature.program.entity.*
import org.junit.jupiter.api.Test
import java.time.*
import kotlin.test.*

class InterventionValidatorTest {
    private val validator = InterventionValidator()
    private val today = LocalDate.of(2026, 10, 19)
    private val user = User(id = 1)
    private val edition = ProgramEdition(program = Program(name = "Programa", createdBy = user), name = "Edicion",
        startDate = today, endDate = today, maxCapacity = 1, createdBy = user)
    private val application = Application(user = user, registeredBy = user, programEdition = edition,
        enrollmentPeriod = EnrollmentPeriod(programEdition = edition, openDate = today, closeDate = today),
        submittedAt = today.atStartOfDay(), status = ApplicationStatus.CLOSED, assignedWorker = user)
    private val plan = InterventionPlan(application = application, responsible = user, createdAt = today.atStartOfDay().atOffset(ZoneOffset.UTC))
    private val objective = InterventionObjective(plan = plan, position = 0, description = "Objetivo")
    private val action = InterventionAction(objective = objective, position = 0, description = "Accion",
        startDate = today.minusDays(10), dueDate = today.minusDays(5))
    private fun follow(status: InterventionFollowUpStatus = InterventionFollowUpStatus.IN_PROGRESS, date: LocalDate = today,
        notes: String = " Notas ", result: String? = null) = CreateInterventionFollowUpRequest(status, date, notes, result)

    @Test fun `estructura admite limites recortados fechas iguales y planificacion futura`() {
        val request = CreateInterventionPlanRequest(listOf(InterventionObjectiveRequest(" " + "x".repeat(1000) + " ",
            listOf(InterventionActionRequest(" Accion ", today.plusDays(3), today.plusDays(3))))))
        val data = validator.validatePlan(request).single()
        assertEquals(1000, data.description.length)
        assertEquals("Accion", data.actions.single().description)
        assertEquals(today.plusDays(3), data.actions.single().dueDate)
    }
    @Test fun `validacion de estructura exige cada nivel y fechas completas`() {
        val validAction = InterventionActionRequest("Accion", today, today)
        listOf(null, "", " ", "x".repeat(1001)).forEach { description ->
            assertFailsWith<BadRequestException> {
                validator.validatePlan(CreateInterventionPlanRequest(listOf(InterventionObjectiveRequest(description, listOf(validAction)))))
            }
        }
        listOf(validAction.copy(startDate = null), validAction.copy(dueDate = null),
            validAction.copy(startDate = today.plusDays(1))).forEach {
            assertFailsWith<BadRequestException> {
                validator.validatePlan(CreateInterventionPlanRequest(listOf(InterventionObjectiveRequest("Objetivo", listOf(it)))))
            }
        }
    }
    @Test fun `fecha real inclusiva admite carga historica y accion vencida pero usa hoy del servidor`() {
        validator.validateFollowUp(follow(date = action.startDate), action, null, today)
        validator.validateFollowUp(follow(date = today), action, null, today)
        assertFailsWith<BadRequestException> { validator.validateFollowUp(follow(date = today), action, null, today.minusDays(1)) }
        assertFailsWith<BadRequestException> { validator.validateFollowUp(follow(date = action.startDate.minusDays(1)), action, null, today) }
        val last = InterventionFollowUp(action = action, sequenceNumber = 1, previousStatus = InterventionActionStatus.PENDING,
            status = InterventionActionStatus.IN_PROGRESS, occurredOn = today.minusDays(1), notes = "Anterior", result = null,
            author = user, recordedAt = OffsetDateTime.now(ZoneOffset.UTC))
        validator.validateFollowUp(follow(date = today.minusDays(1)), action, last, today)
        assertFailsWith<BadRequestException> { validator.validateFollowUp(follow(date = today.minusDays(2)), action, last, today) }
    }
    @Test fun `notas y resultado respetan limites y finalizacion directa requiere resultado`() {
        val completed = validator.validateFollowUp(follow(InterventionFollowUpStatus.COMPLETED, notes = " " + "x".repeat(2000) + " ",
            result = " " + "r".repeat(2000) + " "), action, null, today)
        assertEquals(2000, completed.notes.length); assertEquals(2000, completed.result!!.length)
        assertEquals(InterventionActionStatus.COMPLETED, completed.status)
        listOf(follow(notes = " "), follow(notes = "x".repeat(2001)), follow(result = " "),
            follow(result = "x".repeat(2001)), follow(InterventionFollowUpStatus.COMPLETED)).forEach {
            assertFailsWith<BadRequestException> { validator.validateFollowUp(it, action, null, today) }
        }
        action.status = InterventionActionStatus.COMPLETED
        assertFailsWith<ConflictException> { validator.validateFollowUp(follow(), action, null, today) }
    }
    @Test fun `responsable del plan debe seguir siendo trabajador asignado aun en caso cerrado`() {
        validator.validateCreation(application, user, true)
        validator.validateResponsibility(application, user, plan)
        assertFailsWith<ConflictException> { validator.validateCreation(application, user, false) }
        application.assignedWorker = User(id = 2)
        assertFailsWith<ConflictException> { validator.validateResponsibility(application, User(id = 2), plan) }
        assertFailsWith<ConflictException> { validator.validateResponsibility(application, user, plan) }
        application.assignedWorker = null
        assertFailsWith<ConflictException> { validator.validateResponsibility(application, user, plan) }
    }
}
