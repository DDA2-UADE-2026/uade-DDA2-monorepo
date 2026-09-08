package com.uade.dda2.server.feature.program.dto.available.response

import com.uade.dda2.server.feature.program.entity.enums.ProgramBenefitType
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.util.UUID

@Schema(description = "Beneficio ofrecido por una edición disponible.")
data class AvailableProgramBenefitResponse(
    @field:Schema(description = "UUID del beneficio.", format = "uuid", accessMode = Schema.AccessMode.READ_ONLY)
    val id: UUID,
    @field:Schema(description = "Tipo de beneficio.", example = "HOUSING_SUBSIDY", accessMode = Schema.AccessMode.READ_ONLY)
    val type: ProgramBenefitType,
    @field:Schema(description = "Descripción complementaria del beneficio.", nullable = true, accessMode = Schema.AccessMode.READ_ONLY)
    val description: String?,
    @field:Schema(description = "Monto del beneficio cuando corresponde.", example = "25000.00", nullable = true, accessMode = Schema.AccessMode.READ_ONLY)
    val amount: BigDecimal?,
)
