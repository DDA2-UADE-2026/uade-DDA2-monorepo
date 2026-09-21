package com.uade.dda2.server.feature.appointment.error

import com.uade.dda2.server.error.BadRequestException
import com.uade.dda2.server.error.ConflictException
import com.uade.dda2.server.error.NotFoundException

object AppointmentErrors {
    fun resourceNotFound(): NotFoundException =
        NotFoundException("APPOINTMENT_RESOURCE_NOT_FOUND", "No se encontro el recurso solicitado.")

    fun invalidRequest(message: String): BadRequestException =
        BadRequestException("APPOINTMENT_INVALID_REQUEST", message)

    fun timeInPast(): BadRequestException =
        BadRequestException("APPOINTMENT_TIME_IN_PAST", "Debe seleccionarse una fecha y un horario futuros.")

    fun invalidIdempotencyKey(): BadRequestException =
        BadRequestException(
            "APPOINTMENT_INVALID_IDEMPOTENCY_KEY",
            "Idempotency-Key debe tener entre 1 y 128 caracteres ASCII visibles, sin espacios.",
        )

    fun slotUnavailable(): ConflictException =
        ConflictException(
            "APPOINTMENT_SLOT_UNAVAILABLE",
            "El horario dejo de estar disponible. Actualice los horarios e intente nuevamente.",
        )

    fun citizenOverlap(): ConflictException =
        ConflictException(
            "APPOINTMENT_CITIZEN_OVERLAP",
            "El horario se superpone con otro turno confirmado del ciudadano.",
        )

    fun idempotencyConflict(): ConflictException =
        ConflictException(
            "IDEMPOTENCY_KEY_REUSED",
            "La clave de idempotencia ya se utilizo con otro turno.",
        )
}
