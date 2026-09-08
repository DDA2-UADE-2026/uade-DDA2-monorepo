package com.uade.dda2.server.feature.program.mapper

import com.uade.dda2.server.feature.program.dto.available.response.AvailableProgramBenefitResponse
import com.uade.dda2.server.feature.program.dto.available.response.AvailableEnrollmentPeriodResponse
import com.uade.dda2.server.feature.program.dto.available.response.AvailableProgramDetailResponse
import com.uade.dda2.server.feature.program.dto.available.response.AvailableProgramEditionResponse
import com.uade.dda2.server.feature.program.dto.available.response.AvailableProgramIncompatibilityResponse
import com.uade.dda2.server.feature.program.dto.available.response.AvailableProgramListItemResponse
import com.uade.dda2.server.feature.program.dto.available.response.AvailableProgramRequirementResponse
import com.uade.dda2.server.feature.program.entity.Program
import com.uade.dda2.server.feature.program.entity.ProgramBenefit
import com.uade.dda2.server.feature.program.entity.ProgramEdition
import com.uade.dda2.server.feature.program.entity.ProgramIncompatibility
import com.uade.dda2.server.feature.program.entity.ProgramRequirement
import com.uade.dda2.server.feature.enrollmentperiod.entity.EnrollmentPeriod
import java.util.UUID

fun Program.toAvailableListItemResponse(
    editions: List<ProgramEdition>,
): AvailableProgramListItemResponse {
    val nearestEdition = editions.first()

    return AvailableProgramListItemResponse(
        id = requireNotNull(id),
        name = name,
        objective = objective,
        availableEditions = editions.size,
        nextEditionStartDate = nearestEdition.startDate,
        nextEditionEndDate = nearestEdition.endDate,
    )
}

fun Program.toAvailableDetailResponse(
    editions: List<AvailableProgramEditionResponse>,
    incompatibilities: List<AvailableProgramIncompatibilityResponse>,
): AvailableProgramDetailResponse =
    AvailableProgramDetailResponse(
        id = requireNotNull(id),
        name = name,
        objective = objective,
        editions = editions,
        incompatibilities = incompatibilities,
    )

fun ProgramEdition.toAvailableResponse(
    benefits: List<AvailableProgramBenefitResponse>,
    requirements: List<AvailableProgramRequirementResponse>,
    enrollmentPeriods: List<AvailableEnrollmentPeriodResponse>,
): AvailableProgramEditionResponse =
    AvailableProgramEditionResponse(
        id = requireNotNull(id),
        name = name,
        startDate = startDate,
        endDate = endDate,
        maxCapacity = maxCapacity,
        currentEnrollment = currentEnrollment,
        availableCapacity = (maxCapacity - currentEnrollment).coerceAtLeast(0),
        status = status,
        benefits = benefits,
        requirements = requirements,
        enrollmentPeriods = enrollmentPeriods,
    )

fun EnrollmentPeriod.toAvailableResponse(): AvailableEnrollmentPeriodResponse =
    AvailableEnrollmentPeriodResponse(
        id = requireNotNull(id),
        openDate = openDate,
        closeDate = closeDate,
    )

fun ProgramBenefit.toAvailableResponse(): AvailableProgramBenefitResponse =
    AvailableProgramBenefitResponse(
        id = requireNotNull(id),
        type = benefitType,
        description = description,
        amount = amount,
    )

fun ProgramRequirement.toAvailableResponse(): AvailableProgramRequirementResponse =
    AvailableProgramRequirementResponse(
        id = requireNotNull(id),
        type = type,
        value = value,
        description = description,
    )

fun ProgramIncompatibility.toAvailableResponse(
    requestedProgramId: UUID,
): AvailableProgramIncompatibilityResponse {
    val incompatibleProgram =
        if (program.id == requestedProgramId) {
            incompatibleWithProgram
        } else {
            program
        }

    return AvailableProgramIncompatibilityResponse(
        id = requireNotNull(incompatibleProgram.id),
        name = incompatibleProgram.name,
    )
}
