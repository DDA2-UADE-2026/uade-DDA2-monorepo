package com.uade.dda2.server.feature.application.error

import com.uade.dda2.server.error.BadRequestException
import com.uade.dda2.server.error.ConflictException
import com.uade.dda2.server.error.NotFoundException

object ApplicationVisitRescheduleRequestErrors {
    fun invalidData(message: String): BadRequestException =
        BadRequestException("APPLICATION_VISIT_RESCHEDULE_REQUEST_INVALID_DATA", message)

    fun invalidFilter(message: String): BadRequestException =
        BadRequestException("APPLICATION_VISIT_RESCHEDULE_REQUEST_INVALID_FILTER", message)

    fun notFound(): NotFoundException =
        NotFoundException(
            "APPLICATION_VISIT_RESCHEDULE_REQUEST_NOT_FOUND",
            "Solicitud de reprogramacion no encontrada.",
        )

    fun notAllowed(): ConflictException =
        ConflictException(
            "APPLICATION_VISIT_RESCHEDULE_REQUEST_NOT_ALLOWED",
            "Solo se puede solicitar la reprogramacion de una visita o entrevista programada cuyo inicio sea futuro.",
        )

    fun alreadyPending(): ConflictException =
        ConflictException(
            "APPLICATION_VISIT_RESCHEDULE_REQUEST_ALREADY_PENDING",
            "La visita o entrevista ya tiene una solicitud de reprogramacion pendiente.",
        )

    fun notReviewable(): ConflictException =
        ConflictException(
            "APPLICATION_VISIT_RESCHEDULE_REQUEST_NOT_REVIEWABLE",
            "La solicitud no esta pendiente o ya no puede aprobarse con el estado actual de la visita.",
        )

    fun pending(): ConflictException =
        ConflictException(
            "APPLICATION_VISIT_RESCHEDULE_REQUEST_PENDING",
            "La solicitud de reprogramacion pendiente debe resolverse antes de modificar la visita o entrevista.",
        )
}
