package com.uade.dda2.server.feature.activity.error

import com.uade.dda2.server.error.BadRequestException
import com.uade.dda2.server.error.ConflictException
import com.uade.dda2.server.error.NotFoundException
import com.uade.dda2.server.feature.activity.entity.ActivityStatus
import java.util.UUID

object ActivityErrors {
    fun notFound(id: UUID): NotFoundException =
        NotFoundException(
            code = "ACTIVITY_NOT_FOUND",
            message = "No se encontró la actividad con id '$id'.",
        )

    fun invalidDateRange(): BadRequestException =
        BadRequestException(
            code = "ACTIVITY_INVALID_DATE_RANGE",
            message = "La fecha de finalización no puede ser anterior a la fecha de inicio.",
        )

    fun cannotEdit(status: ActivityStatus): ConflictException =
        ConflictException(
            code = "ACTIVITY_CANNOT_BE_EDITED",
            message = "Solo se pueden editar actividades en borrador. Estado actual: $status.",
        )

    fun invalidStatusTransition(
        currentStatus: ActivityStatus,
        newStatus: ActivityStatus,
    ): ConflictException =
        ConflictException(
            code = "ACTIVITY_INVALID_STATUS_TRANSITION",
            message = "No se puede cambiar la actividad de $currentStatus a $newStatus.",
        )
}
