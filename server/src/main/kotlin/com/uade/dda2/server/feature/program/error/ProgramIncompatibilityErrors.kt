package com.uade.dda2.server.feature.program.error

import com.uade.dda2.server.error.BadRequestException
import com.uade.dda2.server.error.ConflictException
import com.uade.dda2.server.error.NotFoundException
import java.util.UUID

object ProgramIncompatibilityErrors {

    fun sameProgram(): BadRequestException =
        BadRequestException(
            code = "PROGRAM_INCOMPATIBILITY_SAME_PROGRAM",
            message = "Un programa no puede ser incompatible consigo mismo.",
        )

    fun alreadyExists(
        programId: UUID,
        incompatibleProgramId: UUID,
    ): ConflictException =
        ConflictException(
            code = "PROGRAM_INCOMPATIBILITY_ALREADY_EXISTS",
            message = "La incompatibilidad entre los programas '$programId' y '$incompatibleProgramId' ya existe.",
        )

    fun notFound(
        programId: UUID,
        incompatibleProgramId: UUID,
    ): NotFoundException =
        NotFoundException(
            code = "PROGRAM_INCOMPATIBILITY_NOT_FOUND",
            message = "No existe una incompatibilidad entre los programas '$programId' y '$incompatibleProgramId'.",
        )
}
