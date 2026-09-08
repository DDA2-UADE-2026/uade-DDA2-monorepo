package com.uade.dda2.server.feature.program.error

import com.uade.dda2.server.feature.program.entity.enums.ProgramEditionStatus
import com.uade.dda2.server.error.BadRequestException
import com.uade.dda2.server.error.ConflictException
import com.uade.dda2.server.error.NotFoundException
import java.util.UUID

object ProgramEditionErrors {

    fun notFound(id: UUID): NotFoundException =
        NotFoundException(
            code = "PROGRAM_EDITION_NOT_FOUND",
            message = "No se encontró la edición de programa con id '$id'.",
        )

    fun nameAlreadyExists(
        programId: UUID,
        name: String,
    ): ConflictException =
        ConflictException(
            code = "PROGRAM_EDITION_NAME_ALREADY_EXISTS",
            message = "El programa '$programId' ya tiene una edición con el nombre '$name'.",
        )

    fun invalidDateRange(): BadRequestException =
        BadRequestException(
            code = "PROGRAM_EDITION_INVALID_DATE_RANGE",
            message = "La fecha de finalización no puede ser anterior a la fecha de inicio.",
        )

    fun invalidCapacity(): BadRequestException =
        BadRequestException(
            code = "PROGRAM_EDITION_INVALID_CAPACITY",
            message = "La capacidad máxima debe ser mayor a cero.",
        )

    fun capacityBelowCurrentEnrollment(
        currentEnrollment: Int,
    ): BadRequestException =
        BadRequestException(
            code = "PROGRAM_EDITION_CAPACITY_BELOW_ENROLLMENT",
            message = "La capacidad máxima no puede ser menor a la cantidad actual de inscriptos ($currentEnrollment).",
        )

    fun invalidStatusTransition(
        currentStatus: ProgramEditionStatus,
        newStatus: ProgramEditionStatus,
    ): ConflictException =
        ConflictException(
            code = "PROGRAM_EDITION_INVALID_STATUS_TRANSITION",
            message = "No se puede cambiar el estado de la edición de '$currentStatus' a '$newStatus'.",
        )

    fun closedEditionCannotBeModified(): ConflictException =
        ConflictException(
            code = "PROGRAM_EDITION_CLOSED",
            message = "No se puede modificar una edición que se encuentra cerrada.",
        )

    fun closedEditionCannotBeDeleted(): ConflictException =
        ConflictException(
            code = "PROGRAM_EDITION_CLOSED_DELETE_NOT_ALLOWED",
            message = "No se puede eliminar una edición que se encuentra cerrada.",
        )

    fun hasConfiguration(id: UUID): ConflictException =
        ConflictException(
            code = "PROGRAM_EDITION_HAS_CONFIGURATION",
            message = "No se puede eliminar la edición '$id' porque tiene requisitos, beneficios o períodos de inscripción asociados.",
        )

    fun hasOpenEnrollmentPeriod(id: UUID): ConflictException =
        ConflictException(
            code = "PROGRAM_EDITION_HAS_OPEN_ENROLLMENT_PERIOD",
            message = "No se puede suspender o cerrar la edición '$id' mientras tenga un período de inscripción abierto.",
        )

    fun dateRangeExcludesEnrollmentPeriods(id: UUID): ConflictException =
        ConflictException(
            code = "PROGRAM_EDITION_DATE_RANGE_EXCLUDES_ENROLLMENT_PERIODS",
            message = "Las nuevas fechas de la edición '$id' dejan períodos de inscripción fuera de su rango.",
        )

    fun hasEnrollments(
        id: UUID,
        currentEnrollment: Int,
    ): ConflictException =
        ConflictException(
            code = "PROGRAM_EDITION_HAS_ENROLLMENTS",
            message = "No se puede eliminar la edición '$id' porque tiene '$currentEnrollment' inscripciones.",
        )
}
