package com.uade.dda2.server.feature.benefit.dto.response

import com.uade.dda2.server.feature.benefit.entity.BenefitGrantStatus
import com.uade.dda2.server.feature.program.entity.enums.ProgramBenefitType
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Schema(description = "Otorgamiento individual visible solo para administracion. Los datos copiados no siguen los cambios del catalogo.")
data class BenefitGrantResponse(
    val id: UUID,
    val applicationId: UUID,
    val userId: Long,
    val programBenefitId: UUID,
    val benefitType: ProgramBenefitType,
    val description: String?,
    val amount: BigDecimal?,
    val startDate: LocalDate,
    val endDate: LocalDate,
    @field:Schema(description = "Estado actual. EXPIRED y FINALIZED son definitivos; el vencimiento automatico se procesa despues del ultimo dia inclusivo.")
    val status: BenefitGrantStatus,
    val assignedByUserId: Long,
    val assignedByUserName: String,
    @field:Schema(description = "Instante UTC de registro, asignado por el servidor.")
    val assignedAt: OffsetDateTime,
)
