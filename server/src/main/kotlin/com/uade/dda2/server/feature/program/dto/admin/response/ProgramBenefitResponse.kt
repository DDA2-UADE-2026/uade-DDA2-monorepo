package com.uade.dda2.server.feature.program.dto.admin.response

import com.uade.dda2.server.feature.program.entity.enums.ProgramBenefitType
import java.math.BigDecimal
import java.util.UUID

data class ProgramBenefitResponse(
    val id: UUID,
    val programEditionId: UUID,
    val benefitType: ProgramBenefitType,
    val description: String?,
    val amount: BigDecimal?,
)