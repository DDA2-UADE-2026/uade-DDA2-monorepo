package com.uade.dda2.server.feature.application.error

import com.uade.dda2.server.error.BadRequestException
import com.uade.dda2.server.error.ConflictException
import com.uade.dda2.server.error.NotFoundException
import com.uade.dda2.server.error.PayloadTooLargeException

object ApplicationDocumentErrors {
    fun notFound() = NotFoundException("APPLICATION_DOCUMENT_NOT_FOUND", "Documento de solicitud no encontrado.")
    fun finalApplication() = ConflictException("APPLICATION_DOCUMENTS_FINALIZED", "No se pueden modificar documentos de una solicitud resuelta.")
    fun invalidReviewStatus() = ConflictException("APPLICATION_DOCUMENT_INVALID_REVIEW_STATUS", "Solo un documento PENDING puede revisarse como VALID u OBSERVED.")
    fun observationRequired() = ConflictException("APPLICATION_DOCUMENT_OBSERVATION_REQUIRED", "La observación es obligatoria al marcar un documento como OBSERVED.")
    fun observationNotAllowed() = ConflictException("APPLICATION_DOCUMENT_OBSERVATION_NOT_ALLOWED", "Una revisión VALID no admite observación.")
    fun emptyFile() = BadRequestException("APPLICATION_DOCUMENT_EMPTY_FILE", "El archivo no puede estar vacío.")
    fun invalidFileName() = BadRequestException("APPLICATION_DOCUMENT_INVALID_FILE_NAME", "El archivo debe tener un nombre válido.")
    fun invalidFileType() = BadRequestException("APPLICATION_DOCUMENT_INVALID_FILE_TYPE", "Solo se admiten archivos PDF, JPEG o PNG cuyo contenido coincida con su extensión y MIME.")
    fun fileTooLarge() = PayloadTooLargeException("APPLICATION_DOCUMENT_FILE_TOO_LARGE", "El archivo no puede superar 10 MB.")
}
