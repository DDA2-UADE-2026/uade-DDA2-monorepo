package com.uade.dda2.server.feature.benefit.mapper

import com.uade.dda2.server.feature.benefit.dto.response.CitizenBenefitGrantResponse
import com.uade.dda2.server.feature.benefit.entity.BenefitGrant

fun BenefitGrant.toCitizenResponse(): CitizenBenefitGrantResponse {
    val edition = application.programEdition
    val program = edition.program
    return CitizenBenefitGrantResponse(
        id = requireNotNull(id), applicationId = requireNotNull(application.id), applicationNumber = requireNotNull(application.applicationNumber),
        programBenefitId = requireNotNull(programBenefit.id), programId = requireNotNull(program.id), programName = program.name,
        programEditionId = requireNotNull(edition.id), programEditionName = edition.name,
        benefitType = benefitType, description = description, amount = amount, status = status, startDate = startDate, endDate = endDate,
    )
}
