package com.uade.dda2.server.feature.benefit.dto.response

import com.uade.dda2.server.feature.benefit.entity.BenefitGrantChangeAction
import com.uade.dda2.server.feature.benefit.entity.BenefitGrantStatus
import com.uade.dda2.server.feature.program.entity.enums.ProgramRequirementType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Schema(description = "Actuacion inmutable. ASSIGNED conserva los datos conocidos de la asignacion, sin motivo ni estado anterior.")
data class BenefitGrantChangeResponse(
    val id: UUID,
    val grantId: UUID,
    val applicationId: UUID,
    val sequenceNumber: Int,
    val action: BenefitGrantChangeAction,
    val previousStatus: BenefitGrantStatus?,
    val status: BenefitGrantStatus,
    val previousStartDate: LocalDate?,
    val previousEndDate: LocalDate?,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val reason: String?,
    val responsibleUserId: Long,
    val responsibleUserName: String,
    @field:Schema(description = "Instante UTC de la actuacion. La finalizacion tiene efecto inmediato.")
    val changedAt: OffsetDateTime,
    val requirementChecks: List<BenefitGrantChangeRequirementResponse>,
)

data class BenefitGrantChangeRequirementResponse(
    val requirementId: UUID,
    val type: ProgramRequirementType,
    val value: String,
    val description: String?,
    val fulfilled: Boolean,
)
