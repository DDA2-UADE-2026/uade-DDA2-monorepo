package com.uade.dda2.server.feature.application.error

import com.uade.dda2.server.error.BadRequestException
import com.uade.dda2.server.error.ConflictException

object ApplicationResolutionErrors {
    fun invalidData(message: String) = BadRequestException("APPLICATION_RESOLUTION_INVALID_DATA", message)
    fun statusNotAllowed() = conflict("STATUS_NOT_ALLOWED", "El estado actual no permite esta resolucion.")
    fun evaluationRequired() = conflict("EVALUATION_REQUIRED", "La solicitud requiere una evaluacion profesional.")
    fun pendingVisits() = conflict("PENDING_VISITS", "Hay visitas o entrevistas pendientes.")
    fun requirementsNotMet() = conflict("REQUIREMENTS_NOT_MET", "Todos los requisitos deben estar cumplidos.")
    fun documentsNotValid() = conflict("DOCUMENTS_NOT_VALID", "Todos los documentos obligatorios deben estar en VALID.")
    fun editionNotEligible() = conflict("EDITION_NOT_ELIGIBLE", "La edicion debe estar activa y no vencida.")
    fun incompatibleApplication() = conflict("INCOMPATIBLE_APPLICATION", "El titular tiene una aprobacion incompatible con vigencia superpuesta.")
    fun noCapacity() = conflict("NO_CAPACITY", "No hay cupo disponible en la edicion.")
    fun capacityAvailable() = conflict("CAPACITY_AVAILABLE", "Solo se admite lista de espera cuando el cupo esta agotado.")
    private fun conflict(suffix: String, message: String) = ConflictException("APPLICATION_RESOLUTION_$suffix", message)
}
