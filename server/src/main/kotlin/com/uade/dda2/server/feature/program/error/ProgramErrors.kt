package com.uade.dda2.server.feature.program.error

import com.uade.dda2.server.error.ConflictException
import com.uade.dda2.server.error.NotFoundException
import java.util.UUID

object ProgramErrors {

    fun notFound(id: UUID): NotFoundException =
        NotFoundException(
            code = "PROGRAM_NOT_FOUND",
            message = "No se encontró el programa con id '$id'.",
        )

    fun nameAlreadyExists(name: String): ConflictException =
        ConflictException(
            code = "PROGRAM_NAME_ALREADY_EXISTS",
            message = "Ya existe un programa con el nombre '$name'.",
        )

    fun hasEditions(id: UUID): ConflictException =
        ConflictException(
            code = "PROGRAM_HAS_EDITIONS",
            message = "No se puede eliminar el programa '$id' porque tiene ediciones asociadas.",
        )

    fun hasIncompatibilities(id: UUID): ConflictException =
        ConflictException(
            code = "PROGRAM_HAS_INCOMPATIBILITIES",
            message = "No se puede eliminar el programa '$id' porque tiene incompatibilidades asociadas.",
        )
}
