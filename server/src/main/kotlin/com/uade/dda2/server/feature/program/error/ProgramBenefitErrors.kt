package com.uade.dda2.server.feature.program.error

import com.uade.dda2.server.error.BadRequestException
import com.uade.dda2.server.error.NotFoundException
import java.util.UUID

object ProgramBenefitErrors {

    fun notFound(
        id: UUID,
        programEditionId: UUID,
    ): NotFoundException =
        NotFoundException(
            code = "PROGRAM_BENEFIT_NOT_FOUND",
            message = "No se encontró el beneficio '$id' para la edición '$programEditionId'.",
        )

    fun invalidAmount(): BadRequestException =
        BadRequestException(
            code = "PROGRAM_BENEFIT_INVALID_AMOUNT",
            message = "El monto del beneficio no puede ser negativo.",
        )
}
