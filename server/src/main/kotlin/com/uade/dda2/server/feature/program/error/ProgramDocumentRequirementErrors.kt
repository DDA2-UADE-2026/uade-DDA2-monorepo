package com.uade.dda2.server.feature.program.error

import com.uade.dda2.server.error.ConflictException
import com.uade.dda2.server.error.NotFoundException
import java.util.UUID

object ProgramDocumentRequirementErrors {
    fun notFound() = NotFoundException("PROGRAM_DOCUMENT_REQUIREMENT_NOT_FOUND", "Requisito documental no encontrado para la edición indicada.")
    fun duplicateCode(code: String) = ConflictException("PROGRAM_DOCUMENT_REQUIREMENT_CODE_EXISTS", "Ya existe un requisito documental con el código '$code' en esta edición.")
    fun catalogLocked() = ConflictException("PROGRAM_DOCUMENT_REQUIREMENTS_LOCKED", "El catálogo documental no puede modificarse porque la edición ya tiene solicitudes.")
    fun wrongEdition(requirementId: UUID) = NotFoundException("PROGRAM_DOCUMENT_REQUIREMENT_NOT_FOUND", "El requisito documental '$requirementId' no pertenece a la edición de la solicitud.")
}
