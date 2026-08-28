package com.uade.dda2.server.feature.enrollmentperiod.error

import com.uade.dda2.server.error.BadRequestException
import com.uade.dda2.server.error.ConflictException
import com.uade.dda2.server.error.NotFoundException
import com.uade.dda2.server.feature.enrollmentperiod.entity.EnrollmentPeriodStatus
import com.uade.dda2.server.feature.program.entity.enums.ProgramEditionStatus
import java.time.LocalDate
import java.util.UUID

object EnrollmentPeriodErrors {

    fun notFound(id: UUID): NotFoundException =
        NotFoundException(
            code = "ENROLLMENT_PERIOD_NOT_FOUND",
            message = "No se encontró el período de inscripción con id '$id'.",
        )

    fun invalidDateRange(): BadRequestException =
        BadRequestException(
            code = "ENROLLMENT_PERIOD_INVALID_DATE_RANGE",
            message = "La fecha de cierre no puede ser anterior a la fecha de apertura.",
        )

    fun outsideEditionDateRange(
        editionStartDate: LocalDate,
        editionEndDate: LocalDate,
    ): BadRequestException =
        BadRequestException(
            code = "ENROLLMENT_PERIOD_OUTSIDE_EDITION_DATE_RANGE",
            message = "El período debe estar contenido entre '$editionStartDate' y '$editionEndDate', las fechas de la edición.",
        )

    fun overlapsExistingPeriod(): ConflictException =
        ConflictException(
            code = "ENROLLMENT_PERIOD_OVERLAPS_EXISTING_PERIOD",
            message = "El período se superpone con otro período de inscripción de la misma edición.",
        )

    fun closedPeriodCannotBeModified(): ConflictException =
        ConflictException(
            code = "ENROLLMENT_PERIOD_CLOSED",
            message = "No se puede modificar un período de inscripción cerrado.",
        )

    fun invalidStatusTransition(
        currentStatus: EnrollmentPeriodStatus,
        newStatus: EnrollmentPeriodStatus,
    ): ConflictException =
        ConflictException(
            code = "ENROLLMENT_PERIOD_INVALID_STATUS_TRANSITION",
            message = "No se puede cambiar el estado del período de '$currentStatus' a '$newStatus'.",
        )

    fun notOpen(status: EnrollmentPeriodStatus): ConflictException =
        ConflictException(
            code = "ENROLLMENT_PERIOD_NOT_OPEN",
            message = "El período no admite solicitudes porque su estado actual es '$status'.",
        )

    fun alreadyOpenForEdition(): ConflictException =
        ConflictException(
            code = "ENROLLMENT_PERIOD_ALREADY_OPEN_FOR_EDITION",
            message = "Ya existe una convocatoria abierta para esta edición. Debe cerrarla antes de abrir otra.",
        )

    fun outsideActiveDateRange(
        currentDate: LocalDate,
        openDate: LocalDate,
        closeDate: LocalDate,
    ): ConflictException =
        ConflictException(
            code = "ENROLLMENT_PERIOD_OUTSIDE_ACTIVE_DATE_RANGE",
            message = "El período no puede abrirse el '$currentDate'; solo está habilitado entre '$openDate' y '$closeDate'.",
        )

    fun cannotReopenAfterCloseDate(closeDate: LocalDate): ConflictException =
        ConflictException(
            code = "ENROLLMENT_PERIOD_CANNOT_REOPEN_AFTER_CLOSE_DATE",
            message = "El período no puede reabrirse porque su fecha de cierre '$closeDate' ya pasó.",
        )

    fun editionNotActive(status: ProgramEditionStatus): ConflictException =
        ConflictException(
            code = "ENROLLMENT_PERIOD_EDITION_NOT_ACTIVE",
            message = "La edición debe estar activa para abrir o reabrir un período; su estado actual es '$status'.",
        )

    fun programEditionMismatch(
        programId: UUID,
        programEditionId: UUID,
    ): ConflictException =
        ConflictException(
            code = "ENROLLMENT_PERIOD_PROGRAM_EDITION_MISMATCH",
            message = "La edición '$programEditionId' no pertenece al programa '$programId'.",
        )

    fun periodEditionMismatch(
        enrollmentPeriodId: UUID,
        programEditionId: UUID,
    ): ConflictException =
        ConflictException(
            code = "ENROLLMENT_PERIOD_EDITION_MISMATCH",
            message = "El período '$enrollmentPeriodId' no pertenece a la edición '$programEditionId'.",
        )
}
