package com.uade.dda2.server.feature.program.error

import com.uade.dda2.server.feature.program.entity.enums.ProgramRequirementType
import com.uade.dda2.server.error.BadRequestException
import com.uade.dda2.server.error.NotFoundException
import java.util.UUID

object ProgramRequirementErrors {

    fun notFound(
        id: UUID,
        programEditionId: UUID,
    ): NotFoundException =
        NotFoundException(
            code = "PROGRAM_REQUIREMENT_NOT_FOUND",
            message = "No se encontró el requisito '$id' para la edición '$programEditionId'.",
        )

    fun invalidValue(
        type: ProgramRequirementType,
        value: String,
    ): BadRequestException =
        BadRequestException(
            code = "PROGRAM_REQUIREMENT_INVALID_VALUE",
            message = "El valor '$value' no es válido para el requisito '$type'.",
        )
}
