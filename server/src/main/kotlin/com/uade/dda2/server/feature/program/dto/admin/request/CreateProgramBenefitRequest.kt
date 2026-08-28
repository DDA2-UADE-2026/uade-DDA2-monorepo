package com.uade.dda2.server.feature.program.dto.admin.request

import com.uade.dda2.server.feature.program.entity.enums.ProgramBenefitType
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Size
import java.math.BigDecimal

@Schema(description = "Datos requeridos para crear un beneficio.")
data class CreateProgramBenefitRequest(

    @field:Schema(description = "Tipo de beneficio otorgado.", example = "HOUSING_SUBSIDY")
    val benefitType: ProgramBenefitType,

    @field:Schema(description = "Descripción complementaria del beneficio.", example = "Asignación mensual para transporte.", nullable = true)
    @field:Size(
        max = 500,
        message = "La descripción no puede superar los 500 caracteres.",
    )
    val description: String? = null,

    @field:Schema(description = "Monto del beneficio cuando corresponde.", example = "25000.00", minimum = "0", nullable = true)
    @field:DecimalMin(
        value = "0.0",
        inclusive = true,
        message = "El monto del beneficio no puede ser negativo.",
    )
    val amount: BigDecimal? = null,
)
