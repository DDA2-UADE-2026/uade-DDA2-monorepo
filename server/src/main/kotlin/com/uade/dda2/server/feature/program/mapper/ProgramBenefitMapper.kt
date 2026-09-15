package com.uade.dda2.server.feature.program.mapper

import com.uade.dda2.server.feature.program.dto.admin.request.CreateProgramBenefitRequest
import com.uade.dda2.server.feature.program.dto.admin.request.UpdateProgramBenefitRequest
import com.uade.dda2.server.feature.program.dto.admin.response.ProgramBenefitResponse
import com.uade.dda2.server.feature.program.entity.ProgramBenefit
import com.uade.dda2.server.feature.program.entity.ProgramEdition

fun CreateProgramBenefitRequest.toEntity(
    programEdition: ProgramEdition,
): ProgramBenefit =
    ProgramBenefit(
        programEdition = programEdition,
        benefitType = benefitType,
        description = description?.trim(),
        amount = amount,
    )

fun ProgramBenefit.updateFrom(
    request: UpdateProgramBenefitRequest,
) {
    benefitType = request.benefitType
    description = request.description?.trim()
    amount = request.amount
}

fun ProgramBenefit.toResponse(): ProgramBenefitResponse =
    ProgramBenefitResponse(
        id = requireNotNull(id),
        programEditionId = requireNotNull(programEdition.id),
        benefitType = benefitType,
        description = description,
        amount = amount,
    )

fun ProgramBenefit.toAuditSnapshot(): Map<String, Any?> =
    mapOf(
        "id" to id,
        "programEditionId" to programEdition.id,
        "benefitType" to benefitType.name,
        "description" to description,
        "amount" to amount?.toString(),
    )