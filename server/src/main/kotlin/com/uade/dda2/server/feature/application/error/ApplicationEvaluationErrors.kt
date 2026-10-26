package com.uade.dda2.server.feature.application.error

import com.uade.dda2.server.error.BadRequestException
import com.uade.dda2.server.error.ConflictException
import com.uade.dda2.server.error.NotFoundException

object ApplicationEvaluationErrors {
    fun invalidData(message: String): BadRequestException =
        BadRequestException("APPLICATION_EVALUATION_INVALID_DATA", message)

    fun alreadyExists(): ConflictException =
        ConflictException("APPLICATION_EVALUATION_ALREADY_EXISTS", "La solicitud ya tiene una evaluacion registrada.")

    fun statusNotAllowed(): ConflictException =
        ConflictException(
            "APPLICATION_EVALUATION_STATUS_NOT_ALLOWED",
            "El estado actual de la solicitud no permite registrar una evaluacion.",
        )

    fun assignedToAnotherWorker(): ConflictException =
        ConflictException(
            "APPLICATION_EVALUATION_ASSIGNED_TO_ANOTHER_WORKER",
            "La solicitud esta asignada a otro trabajador.",
        )

    fun ownApplication(): ConflictException =
        ConflictException(
            "APPLICATION_EVALUATION_OWN_APPLICATION",
            "No podes evaluar una solicitud en la que sos titular.",
        )

    fun notFound(): NotFoundException =
        NotFoundException("APPLICATION_EVALUATION_NOT_FOUND", "Evaluacion social no encontrada.")

    fun invalidFilter(message: String): BadRequestException =
        BadRequestException("APPLICATION_EVALUATION_INVALID_FILTER", message)
}
