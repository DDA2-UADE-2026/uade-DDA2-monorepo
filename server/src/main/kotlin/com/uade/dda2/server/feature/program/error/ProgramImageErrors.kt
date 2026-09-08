package com.uade.dda2.server.feature.program.error

import com.uade.dda2.server.error.BadRequestException
import com.uade.dda2.server.error.ConflictException
import com.uade.dda2.server.error.NotFoundException
import com.uade.dda2.server.error.PayloadTooLargeException
import java.util.UUID

object ProgramImageErrors {
    fun notFound(programId: UUID? = null): NotFoundException =
        NotFoundException(
            code = "PROGRAM_IMAGE_NOT_FOUND",
            message = programId?.let { "No se encontró una imagen para el programa '$it'." }
                ?: "No se encontró la imagen solicitada.",
        )

    fun alreadyExists(programId: UUID): ConflictException =
        ConflictException(
            code = "PROGRAM_IMAGE_ALREADY_EXISTS",
            message = "El programa '$programId' ya tiene una imagen asociada.",
        )

    fun emptyFile() =
        BadRequestException("PROGRAM_IMAGE_EMPTY_FILE", "La imagen no puede estar vacía.")

    fun invalidFileName() =
        BadRequestException("PROGRAM_IMAGE_INVALID_FILE_NAME", "La imagen debe tener un nombre de archivo válido.")

    fun invalidFileType() =
        BadRequestException(
            "PROGRAM_IMAGE_INVALID_FILE_TYPE",
            "Solo se admiten imágenes JPEG o PNG cuyo contenido coincida con su extensión y MIME.",
        )

    fun fileTooLarge() =
        PayloadTooLargeException("PROGRAM_IMAGE_FILE_TOO_LARGE", "La imagen no puede superar 10 MB.")
}
