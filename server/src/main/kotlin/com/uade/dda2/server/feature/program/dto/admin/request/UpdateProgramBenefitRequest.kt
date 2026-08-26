package com.uade.dda2.server.feature.program.dto.admin.request

import com.uade.dda2.server.feature.program.entity.enums.ProgramBenefitType
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Size
import java.math.BigDecimal

@Schema(description = "Datos requeridos para actualizar un beneficio.")
data class UpdateProgramBenefitRequest(

    @field:Schema(description = "Tipo actualizado del beneficio.", example = "MONETARY")
    val benefitType: ProgramBenefitType,

    @field:Schema(description = "Descripción actualizada del beneficio.", example = "Asignación mensual para transporte y materiales.", nullable = true)
    @field:Size(
        max = 500,
        message = "La descripción no puede superar los 500 caracteres.",
    )
    val description: String? = null,

    @field:Schema(description = "Monto actualizado del beneficio cuando corresponde.", example = "30000.00", minimum = "0", nullable = true)
    @field:DecimalMin(
        value = "0.0",
        inclusive = true,
        message = "El monto del beneficio no puede ser negativo.",
    )
    val amount: BigDecimal? = null,
)
