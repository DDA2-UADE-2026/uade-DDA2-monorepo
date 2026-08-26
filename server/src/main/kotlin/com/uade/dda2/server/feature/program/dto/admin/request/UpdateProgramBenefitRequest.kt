package com.uade.dda2.server.feature.program.dto.admin.request

import com.uade.dda2.server.feature.program.entity.enums.ProgramBenefitType
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Size
import java.math.BigDecimal

data class UpdateProgramBenefitRequest(

    val benefitType: ProgramBenefitType,

    @field:Size(
        max = 500,
        message = "La descripción no puede superar los 500 caracteres.",
    )
    val description: String? = null,

    @field:DecimalMin(
        value = "0.0",
        inclusive = true,
        message = "El monto del beneficio no puede ser negativo.",
    )
    val amount: BigDecimal? = null,
)