package com.uade.dda2.server.feature.application.error

import com.uade.dda2.server.error.ConflictException
import com.uade.dda2.server.error.NotFoundException

object ApplicationErrors {
    fun notFound() = NotFoundException("APPLICATION_NOT_FOUND", "Solicitud no encontrada.")
    fun periodNotFound() = NotFoundException("APPLICATION_ENROLLMENT_PERIOD_NOT_FOUND", "Convocatoria no encontrada.")
    fun periodNotOpen() = ConflictException("APPLICATION_ENROLLMENT_PERIOD_NOT_OPEN", "La convocatoria no está abierta.")
    fun outsidePeriod() = ConflictException("APPLICATION_OUTSIDE_ENROLLMENT_PERIOD", "La fecha actual no está dentro de la convocatoria.")
    fun editionNotActive() = ConflictException("APPLICATION_PROGRAM_EDITION_NOT_ACTIVE", "La edición no está activa.")
    fun duplicatePeriod() = ConflictException("APPLICATION_ALREADY_EXISTS_FOR_PERIOD", "Ya presentaste una solicitud en esta convocatoria.")
    fun blockingApplication() = ConflictException("APPLICATION_ALREADY_EXISTS_FOR_EDITION", "Existe una solicitud de esta edición que aún impide una nueva presentación.")
    fun idempotencyConflict() = ConflictException("APPLICATION_IDEMPOTENCY_CONFLICT", "La clave de idempotencia ya se utilizó con otra convocatoria.")
}
