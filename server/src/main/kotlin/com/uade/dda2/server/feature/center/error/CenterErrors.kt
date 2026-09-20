package com.uade.dda2.server.feature.center.error

import com.uade.dda2.server.error.ConflictException
import com.uade.dda2.server.error.BadRequestException
import com.uade.dda2.server.error.NotFoundException
import java.util.UUID

object CenterErrors {
    fun centerNotFound(id: UUID): NotFoundException =
        NotFoundException("MUNICIPAL_CENTER_NOT_FOUND", "No se encontró el centro municipal con id '$id'.")

    fun centerNameAlreadyExists(): ConflictException =
        ConflictException("MUNICIPAL_CENTER_NAME_ALREADY_EXISTS", "Ya existe un centro municipal con ese nombre.")

    fun centerAlreadyActive(): ConflictException =
        ConflictException("MUNICIPAL_CENTER_ALREADY_ACTIVE", "El centro municipal ya se encuentra activo.")

    fun centerAlreadyInactive(): ConflictException =
        ConflictException("MUNICIPAL_CENTER_ALREADY_INACTIVE", "El centro municipal ya se encuentra inactivo.")

    fun serviceNotFound(id: UUID): NotFoundException =
        NotFoundException("MUNICIPAL_SERVICE_NOT_FOUND", "No se encontró el servicio municipal con id '$id'.")

    fun serviceNameAlreadyExists(): ConflictException =
        ConflictException("MUNICIPAL_SERVICE_NAME_ALREADY_EXISTS", "Ya existe un servicio municipal con ese nombre.")

    fun serviceAlreadyActive(): ConflictException =
        ConflictException("MUNICIPAL_SERVICE_ALREADY_ACTIVE", "El servicio municipal ya se encuentra activo.")

    fun serviceAlreadyInactive(): ConflictException =
        ConflictException("MUNICIPAL_SERVICE_ALREADY_INACTIVE", "El servicio municipal ya se encuentra inactivo.")

    fun centerServiceNotFound(id: UUID): NotFoundException =
        NotFoundException("CENTER_SERVICE_NOT_FOUND", "No se encontró el servicio del centro con id '$id'.")

    fun centerServiceAlreadyActive(): ConflictException =
        ConflictException("CENTER_SERVICE_ALREADY_ACTIVE", "El servicio ya se encuentra asignado al centro.")

    fun centerServiceAlreadyInactive(): ConflictException =
        ConflictException("CENTER_SERVICE_ALREADY_INACTIVE", "El servicio del centro ya se encuentra inactivo.")

    fun inactiveDependency(name: String): ConflictException =
        ConflictException("CENTER_DEPENDENCY_INACTIVE", "No se puede completar la operación porque $name está inactivo.")

    fun professionalNotFound(id: Long): NotFoundException =
        NotFoundException("PROFESSIONAL_NOT_FOUND", "No se encontró el profesional con id '$id'.")

    fun professionalNotEligible(): ConflictException =
        ConflictException(
            "PROFESSIONAL_NOT_ELIGIBLE",
            "El usuario debe estar activo y tener el rol PROFESIONAL_CENTRO.",
        )

    fun professionalAssignmentNotFound(id: UUID): NotFoundException =
        NotFoundException("PROFESSIONAL_ASSIGNMENT_NOT_FOUND", "No se encontró la asignación profesional con id '$id'.")

    fun professionalAssignmentAlreadyActive(): ConflictException =
        ConflictException("PROFESSIONAL_ASSIGNMENT_ALREADY_ACTIVE", "El profesional ya está asignado a este servicio.")

    fun professionalAssignmentAlreadyInactive(): ConflictException =
        ConflictException("PROFESSIONAL_ASSIGNMENT_ALREADY_INACTIVE", "La asignación profesional ya está inactiva.")

    fun openingHourNotFound(id: UUID): NotFoundException =
        NotFoundException("CENTER_OPENING_HOUR_NOT_FOUND", "No se encontró el horario de apertura con id '$id'.")

    fun invalidTimeRange(): BadRequestException =
        BadRequestException("CENTER_OPENING_HOUR_INVALID_RANGE", "La hora de inicio debe ser anterior a la hora de fin.")

    fun openingHourOverlap(): ConflictException =
        ConflictException("CENTER_OPENING_HOUR_OVERLAP", "El horario se superpone con otra franja activa del centro.")

    fun openingHourOverlapOnDay(day: java.time.DayOfWeek): ConflictException =
        ConflictException(
            "CENTER_OPENING_HOUR_OVERLAP",
            "El horario se superpone con otra franja activa del centro el día $day.",
        )

    fun invalidBulkDays(): BadRequestException =
        BadRequestException(
            "CENTER_OPENING_HOUR_INVALID_DAYS",
            "Se requiere al menos un día distinto para crear las franjas de apertura.",
        )

    fun openingHourAlreadyActive(): ConflictException =
        ConflictException("CENTER_OPENING_HOUR_ALREADY_ACTIVE", "El horario de apertura ya está activo.")

    fun openingHourAlreadyInactive(): ConflictException =
        ConflictException("CENTER_OPENING_HOUR_ALREADY_INACTIVE", "El horario de apertura ya está inactivo.")

    fun availabilityWouldLoseCoverage(): ConflictException =
        ConflictException(
            "PROFESSIONAL_AVAILABILITY_OUTSIDE_OPENING_HOURS",
            "El cambio dejaría una disponibilidad profesional fuera del horario de apertura del centro.",
        )

    fun professionalAvailabilityNotFound(id: UUID): NotFoundException =
        NotFoundException(
            "PROFESSIONAL_AVAILABILITY_NOT_FOUND",
            "No se encontró la disponibilidad profesional con id '$id'.",
        )

    fun invalidAvailabilityRange(): BadRequestException =
        BadRequestException(
            "PROFESSIONAL_AVAILABILITY_INVALID_RANGE",
            "La hora de inicio debe ser anterior a la hora de fin.",
        )

    fun professionalAvailabilityOverlap(): ConflictException =
        ConflictException(
            "PROFESSIONAL_AVAILABILITY_OVERLAP",
            "La disponibilidad se superpone con otra franja efectiva del profesional.",
        )

    fun professionalAvailabilityOutsideOpeningHours(): ConflictException =
        ConflictException(
            "PROFESSIONAL_AVAILABILITY_OUTSIDE_OPENING_HOURS",
            "La disponibilidad debe quedar cubierta por el horario de apertura del centro.",
        )

    fun professionalAvailabilityAlreadyActive(): ConflictException =
        ConflictException("PROFESSIONAL_AVAILABILITY_ALREADY_ACTIVE", "La disponibilidad profesional ya está activa.")

    fun professionalAvailabilityOverlapOnDay(day: java.time.DayOfWeek): ConflictException =
        ConflictException(
            "PROFESSIONAL_AVAILABILITY_OVERLAP",
            "La disponibilidad se superpone con otra franja efectiva del profesional el día $day.",
        )

    fun professionalAvailabilityOutsideOpeningHoursOnDay(day: java.time.DayOfWeek): ConflictException =
        ConflictException(
            "PROFESSIONAL_AVAILABILITY_OUTSIDE_OPENING_HOURS",
            "La disponibilidad quedaría fuera del horario de apertura del centro el día $day.",
        )

    fun invalidBulkAvailabilityDays(): BadRequestException =
        BadRequestException(
            "PROFESSIONAL_AVAILABILITY_INVALID_DAYS",
            "Se requiere al menos un día distinto para crear las disponibilidades.",
        )

    fun professionalAvailabilityAlreadyInactive(): ConflictException =
        ConflictException("PROFESSIONAL_AVAILABILITY_ALREADY_INACTIVE", "La disponibilidad profesional ya está inactiva.")
}
