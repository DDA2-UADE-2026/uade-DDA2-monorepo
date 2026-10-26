package com.uade.dda2.server.feature.application.error

import com.uade.dda2.server.error.BadRequestException
import com.uade.dda2.server.error.ConflictException
import com.uade.dda2.server.error.ForbiddenException
import com.uade.dda2.server.error.NotFoundException

object ApplicationErrors {
    fun applicantNotFound() = NotFoundException("APPLICATION_USER_NOT_FOUND", "Usuario solicitante no encontrado.")
    fun assistedSelfNotAllowed() = ForbiddenException("APPLICATION_ASSISTED_SELF_NOT_ALLOWED", "Para solicitar a nombre propio se debe utilizar la presentación propia con un rol habilitado.")
    fun notFound() = NotFoundException("APPLICATION_NOT_FOUND", "Solicitud no encontrada.")
    fun periodNotFound() = NotFoundException("APPLICATION_ENROLLMENT_PERIOD_NOT_FOUND", "Convocatoria no encontrada.")
    fun periodNotOpen() = ConflictException("APPLICATION_ENROLLMENT_PERIOD_NOT_OPEN", "La convocatoria no está abierta.")
    fun outsidePeriod() = ConflictException("APPLICATION_OUTSIDE_ENROLLMENT_PERIOD", "La fecha actual no está dentro de la convocatoria.")
    fun editionNotActive() = ConflictException("APPLICATION_PROGRAM_EDITION_NOT_ACTIVE", "La edición no está activa.")
    fun duplicatePeriod() = ConflictException("APPLICATION_ALREADY_EXISTS_FOR_PERIOD", "Ya presentaste una solicitud en esta convocatoria.")
    fun blockingApplication() = ConflictException("APPLICATION_ALREADY_EXISTS_FOR_EDITION", "Existe una solicitud de esta edición que aún impide una nueva presentación.")
    fun idempotencyConflict() = ConflictException("APPLICATION_IDEMPOTENCY_CONFLICT", "La clave de idempotencia ya se utilizó con otra convocatoria.")
    fun invalidAssignment(message: String) = BadRequestException("APPLICATION_ASSIGNMENT_INVALID", message)
    fun assigneeNotFound() = NotFoundException("APPLICATION_ASSIGNEE_NOT_FOUND", "El usuario a asignar no existe o está inactivo.")
    fun assigneeNotEligible() = ConflictException("APPLICATION_ASSIGNEE_NOT_ELIGIBLE", "El usuario no tiene un rol habilitado para evaluar la solicitud y gestionar sus visitas.")
    fun assignmentStatusNotAllowed() = ConflictException("APPLICATION_ASSIGNMENT_STATUS_NOT_ALLOWED", "El estado actual de la solicitud no permite cambiar el trabajador asignado.")
    fun unassignmentAfterEvaluation() = ConflictException("APPLICATION_UNASSIGNMENT_EVALUATED", "La solicitud ya fue evaluada: necesita un trabajador asignado para gestionar sus visitas.")
    fun invalidResolution(message: String) = BadRequestException("APPLICATION_RESOLUTION_INVALID", message)
    fun resolutionStatusNotAllowed() = ConflictException("APPLICATION_RESOLUTION_STATUS_NOT_ALLOWED", "La solicitud no está en una etapa que admita esta resolución.")
    fun resolutionRequiresEvaluation() = ConflictException("APPLICATION_RESOLUTION_REQUIRES_EVALUATION", "La solicitud necesita una evaluación social registrada antes de resolverse.")
    fun resolutionWithScheduledVisits() = ConflictException("APPLICATION_RESOLUTION_SCHEDULED_VISITS", "La solicitud tiene visitas o entrevistas programadas. Cancelalas o registrá su resultado antes de resolverla.")
    fun resolutionWithPendingDocuments() = ConflictException("APPLICATION_RESOLUTION_PENDING_DOCUMENTS", "La solicitud tiene documentos obligatorios faltantes u observados: no se puede aprobar.")
    fun resolutionAssignedToAnotherWorker() = ForbiddenException("APPLICATION_RESOLUTION_ASSIGNED_TO_ANOTHER_WORKER", "Solo el trabajador asignado puede resolver la solicitud.")
    fun resolutionOwnApplication() = ForbiddenException("APPLICATION_RESOLUTION_OWN_APPLICATION", "No podés resolver una solicitud en la que sos titular.")
    fun editionCapacityReached() = ConflictException("APPLICATION_EDITION_CAPACITY_REACHED", "La edición alcanzó su cupo máximo de solicitudes aprobadas. Podés dejarla en lista de espera.")
    fun assignmentWithScheduledVisits() = ConflictException("APPLICATION_ASSIGNMENT_SCHEDULED_VISITS", "La solicitud tiene visitas o entrevistas programadas. Cancelalas o registrá su resultado antes de reasignarla.")
}
