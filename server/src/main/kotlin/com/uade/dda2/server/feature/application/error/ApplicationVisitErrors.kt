package com.uade.dda2.server.feature.application.error

import com.uade.dda2.server.error.BadRequestException
import com.uade.dda2.server.error.ConflictException
import com.uade.dda2.server.error.NotFoundException

object ApplicationVisitErrors {
    fun invalidData(message: String): BadRequestException =
        BadRequestException("APPLICATION_VISIT_INVALID_DATA", message)

    fun invalidResultData(message: String): BadRequestException =
        BadRequestException("APPLICATION_VISIT_RESULT_INVALID_DATA", message)

    fun invalidFilter(message: String): BadRequestException =
        BadRequestException("APPLICATION_VISIT_INVALID_FILTER", message)

    fun notFound(): NotFoundException =
        NotFoundException("APPLICATION_VISIT_NOT_FOUND", "Visita o entrevista social no encontrada.")

    fun evaluationRequired(): ConflictException =
        ConflictException(
            "APPLICATION_VISIT_EVALUATION_REQUIRED",
            "La solicitud debe tener una evaluacion social antes de programar una visita o entrevista.",
        )

    fun statusNotAllowed(): ConflictException =
        ConflictException(
            "APPLICATION_VISIT_STATUS_NOT_ALLOWED",
            "El estado actual de la solicitud no permite programar una visita o entrevista.",
        )

    fun assignedToAnotherWorker(): ConflictException =
        ConflictException(
            "APPLICATION_VISIT_ASSIGNED_TO_ANOTHER_WORKER",
            "La solicitud no esta asignada al trabajador autenticado.",
        )

    fun notModifiable(): ConflictException =
        ConflictException(
            "APPLICATION_VISIT_NOT_MODIFIABLE",
            "Solo se puede modificar una visita o entrevista programada cuyo inicio sea futuro.",
        )

    fun resultNotRegistrable(): ConflictException =
        ConflictException(
            "APPLICATION_VISIT_RESULT_NOT_REGISTRABLE",
            "Solo se puede registrar el resultado de una visita o entrevista programada que ya finalizo.",
        )

    fun concurrentModification(): ConflictException =
        ConflictException(
            "APPLICATION_VISIT_CONCURRENT_MODIFICATION",
            "La visita o entrevista fue modificada por otra operacion.",
        )

    fun responsibleConflict(): ConflictException =
        ConflictException(
            "APPLICATION_VISIT_RESPONSIBLE_CONFLICT",
            "El responsable ya tiene otra actividad programada en ese horario.",
        )

    fun applicantConflict(): ConflictException =
        ConflictException(
            "APPLICATION_VISIT_APPLICANT_CONFLICT",
            "El ciudadano ya tiene otra actividad programada en ese horario.",
        )
}
