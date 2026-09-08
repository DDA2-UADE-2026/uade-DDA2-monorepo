package com.uade.dda2.server.feature.program.service

import com.uade.dda2.server.feature.program.dto.available.response.AvailableProgramDetailResponse
import com.uade.dda2.server.feature.program.dto.available.response.AvailableProgramListResponse
import com.uade.dda2.server.feature.enrollmentperiod.entity.EnrollmentPeriodStatus
import com.uade.dda2.server.feature.enrollmentperiod.repository.EnrollmentPeriodRepository
import com.uade.dda2.server.feature.program.entity.ProgramBenefit
import com.uade.dda2.server.feature.program.entity.ProgramRequirement
import com.uade.dda2.server.feature.program.entity.enums.ProgramEditionStatus
import com.uade.dda2.server.feature.program.error.ProgramErrors
import com.uade.dda2.server.feature.program.mapper.toAvailableDetailResponse
import com.uade.dda2.server.feature.program.mapper.toAvailableListItemResponse
import com.uade.dda2.server.feature.program.mapper.toAvailableResponse
import com.uade.dda2.server.feature.program.repository.ProgramBenefitRepository
import com.uade.dda2.server.feature.program.repository.ProgramEditionRepository
import com.uade.dda2.server.feature.program.repository.ProgramIncompatibilityRepository
import com.uade.dda2.server.feature.program.repository.ProgramRepository
import com.uade.dda2.server.feature.program.repository.ProgramRequirementRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class AvailableProgramService(
    private val programRepository: ProgramRepository,
    private val programEditionRepository: ProgramEditionRepository,
    private val programBenefitRepository: ProgramBenefitRepository,
    private val programRequirementRepository: ProgramRequirementRepository,
    private val programIncompatibilityRepository: ProgramIncompatibilityRepository,
    private val enrollmentPeriodRepository: EnrollmentPeriodRepository,
) {

    @Transactional(readOnly = true)
    fun list(
        page: Int,
        size: Int,
    ): AvailableProgramListResponse {
        val today = LocalDate.now()
        val programs = programRepository.findAvailable(
            status = ProgramEditionStatus.ACTIVE,
            fromDate = today,
            pageable = PageRequest.of(page, size),
        )
        val programIds = programs.content.map { requireNotNull(it.id) }
        val editionsByProgram = if (programIds.isEmpty()) {
            emptyMap()
        } else {
            programEditionRepository
                .findAllByProgramIdInAndStatusAndEndDateGreaterThanEqualOrderByStartDateAsc(
                    programIds = programIds,
                    status = ProgramEditionStatus.ACTIVE,
                    fromDate = today,
                )
                .groupBy { requireNotNull(it.program.id) }
        }

        return AvailableProgramListResponse(
            content = programs.content.map { program ->
                val programId = requireNotNull(program.id)
                program.toAvailableListItemResponse(
                    editions = requireNotNull(editionsByProgram[programId]),
                )
            },
            page = programs.number,
            size = programs.size,
            totalElements = programs.totalElements,
            totalPages = programs.totalPages,
        )
    }

    @Transactional(readOnly = true)
    fun get(id: UUID): AvailableProgramDetailResponse {
        val today = LocalDate.now()
        val program = programRepository.findAvailableById(
            id = id,
            status = ProgramEditionStatus.ACTIVE,
            fromDate = today,
        ) ?: throw ProgramErrors.notFound(id)

        val editions = programEditionRepository
            .findAllByProgramIdAndStatusAndEndDateGreaterThanEqualOrderByStartDateAsc(
                programId = id,
                status = ProgramEditionStatus.ACTIVE,
                fromDate = today,
            )
        val editionIds = editions.map { requireNotNull(it.id) }
        val benefitsByEdition = programBenefitRepository
            .findAllByProgramEditionIdIn(editionIds)
            .sortedWith(compareBy(ProgramBenefit::benefitType, ProgramBenefit::description))
            .groupBy { requireNotNull(it.programEdition.id) }
        val requirementsByEdition = programRequirementRepository
            .findAllByProgramEditionIdIn(editionIds)
            .sortedWith(compareBy(ProgramRequirement::type, ProgramRequirement::description))
            .groupBy { requireNotNull(it.programEdition.id) }
        val enrollmentPeriodsByEdition = enrollmentPeriodRepository
            .findAllByProgramEditionIdInAndStatusAndOpenDateLessThanEqualAndCloseDateGreaterThanEqualOrderByOpenDateAsc(
                programEditionIds = editionIds,
                status = EnrollmentPeriodStatus.OPEN,
                openDate = today,
                closeDate = today,
            )
            .groupBy { requireNotNull(it.programEdition.id) }

        val editionResponses = editions.map { edition ->
            val editionId = requireNotNull(edition.id)
            edition.toAvailableResponse(
                benefits = benefitsByEdition[editionId].orEmpty().map { it.toAvailableResponse() },
                requirements = requirementsByEdition[editionId].orEmpty().map { it.toAvailableResponse() },
                enrollmentPeriods = enrollmentPeriodsByEdition[editionId].orEmpty().map { it.toAvailableResponse() },
            )
        }
        val incompatibilities = programIncompatibilityRepository
            .findAllByProgramId(id)
            .map { it.toAvailableResponse(requestedProgramId = id) }
            .sortedBy { it.name }

        return program.toAvailableDetailResponse(
            editions = editionResponses,
            incompatibilities = incompatibilities,
        )
    }
}
