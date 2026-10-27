package com.uade.dda2.server.feature.appointment.error

import com.uade.dda2.server.error.BadRequestException
import com.uade.dda2.server.error.ConflictException
import com.uade.dda2.server.error.NotFoundException

object AttentionErrors {
    fun invalidData(message: String): BadRequestException = BadRequestException("ATTENTION_INVALID_DATA", message)

    fun notRegistrable(): ConflictException =
        ConflictException("ATTENTION_NOT_REGISTRABLE", "El turno debe estar confirmado y haber comenzado.")

    fun alreadyRegistered(): ConflictException =
        ConflictException("ATTENTION_ALREADY_REGISTERED", "El turno ya tiene una constancia; actualice el registro existente.")

    fun changed(): ConflictException =
        ConflictException("ATTENTION_CHANGED", "La constancia cambio. Consulte su version vigente antes de reintentar.")

    fun notFound(): NotFoundException = NotFoundException("ATTENTION_NOT_FOUND", "No se encontro la constancia solicitada.")
}
