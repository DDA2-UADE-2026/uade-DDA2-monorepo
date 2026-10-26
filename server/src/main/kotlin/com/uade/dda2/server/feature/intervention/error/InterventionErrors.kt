package com.uade.dda2.server.feature.intervention.error

import com.uade.dda2.server.error.*

object InterventionErrors {
    fun invalidPlan(message: String) = BadRequestException("INTERVENTION_PLAN_INVALID_DATA", message)
    fun invalidFollowUp(message: String) = BadRequestException("INTERVENTION_FOLLOW_UP_INVALID_DATA", message)
    fun invalidPublication(message: String) = BadRequestException("INTERVENTION_PUBLICATION_INVALID_DATA", message)
    fun followUpNotFound() = NotFoundException("INTERVENTION_FOLLOW_UP_NOT_FOUND", "Seguimiento inexistente o ajeno a la accion.")
    fun planNotFound() = NotFoundException("INTERVENTION_PLAN_NOT_FOUND", "La solicitud no tiene plan de intervencion.")
    fun actionNotFound() = NotFoundException("INTERVENTION_ACTION_NOT_FOUND", "Accion inexistente o ajena al plan.")
    fun alreadyExists() = ConflictException("INTERVENTION_PLAN_ALREADY_EXISTS", "La solicitud ya tiene un plan de intervencion.")
    fun evaluationRequired() = ConflictException("INTERVENTION_PLAN_EVALUATION_REQUIRED", "Se requiere evaluacion profesional registrada.")
    fun workerRequired() = ConflictException("INTERVENTION_PLAN_WORKER_REQUIRED", "El caso debe tener trabajador asignado.")
    fun differentWorker() = ConflictException("INTERVENTION_PLAN_ASSIGNED_TO_ANOTHER_WORKER", "Solo el trabajador asignado y responsable puede realizar esta operacion.")
    fun statusNotAllowed() = ConflictException("INTERVENTION_PLAN_STATUS_NOT_ALLOWED", "El estado de la solicitud no admite crear el plan.")
    fun completed() = ConflictException("INTERVENTION_ACTION_ALREADY_COMPLETED", "La accion esta completada y no admite mas seguimientos.")
}
