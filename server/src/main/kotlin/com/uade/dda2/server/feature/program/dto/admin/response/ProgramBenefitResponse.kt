package com.uade.dda2.server.feature.program.dto.admin.response

import com.uade.dda2.server.feature.program.entity.enums.ProgramBenefitType
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.util.UUID

@Schema(description = "Beneficio asociado a una edición de programa.")
data class ProgramBenefitResponse(
    @field:Schema(description = "UUID del beneficio.", example = "750e8400-e29b-41d4-a716-446655440002", format = "uuid", accessMode = Schema.AccessMode.READ_ONLY)
    val id: UUID,
    @field:Schema(description = "UUID de la edición que ofrece el beneficio.", example = "650e8400-e29b-41d4-a716-446655440001", format = "uuid", accessMode = Schema.AccessMode.READ_ONLY)
    val programEditionId: UUID,
    @field:Schema(description = "Tipo de beneficio.", example = "MONETARY", accessMode = Schema.AccessMode.READ_ONLY)
    val benefitType: ProgramBenefitType,
    @field:Schema(description = "Descripción complementaria del beneficio.", example = "Asignación mensual para transporte.", nullable = true, accessMode = Schema.AccessMode.READ_ONLY)
    val description: String?,
    @field:Schema(description = "Monto del beneficio cuando corresponde.", example = "25000.00", minimum = "0", nullable = true, accessMode = Schema.AccessMode.READ_ONLY)
    val amount: BigDecimal?,
)
