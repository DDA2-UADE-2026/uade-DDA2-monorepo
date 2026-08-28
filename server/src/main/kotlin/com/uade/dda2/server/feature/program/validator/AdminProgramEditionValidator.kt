package com.uade.dda2.server.feature.program.validator

import com.uade.dda2.server.feature.program.dto.admin.request.CreateProgramEditionRequest
import com.uade.dda2.server.feature.program.dto.admin.request.UpdateProgramEditionRequest
import com.uade.dda2.server.feature.program.entity.Program
import com.uade.dda2.server.feature.program.entity.ProgramEdition
import com.uade.dda2.server.feature.program.entity.enums.ProgramEditionStatus
import com.uade.dda2.server.feature.program.error.ProgramEditionErrors
import com.uade.dda2.server.feature.program.repository.ProgramBenefitRepository
import com.uade.dda2.server.feature.program.repository.ProgramEditionRepository
import com.uade.dda2.server.feature.program.repository.ProgramRequirementRepository
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.UUID

@Component
class AdminProgramEditionValidator(
    private val programEditionRepository: ProgramEditionRepository,
    private val programBenefitRepository: ProgramBenefitRepository,
    private val programRequirementRepository: ProgramRequirementRepository,
) {

    fun validateCreate(
        program: Program,
        request: CreateProgramEditionRequest,
    ) {
        val programId = requireNotNull(program.id)

        validateUniqueName(
            programId = programId,
            name = request.name,
        )

        validateDateRange(
            startDate = request.startDate,
            endDate = request.endDate,
        )

        validateCapacity(request.maxCapacity)
    }

    fun validateUpdate(
        edition: ProgramEdition,
        request: UpdateProgramEditionRequest,
    ) {
        validateCanModify(edition)

        val editionId = requireNotNull(edition.id)
        val programId = requireNotNull(edition.program.id)

        validateUniqueName(
            programId = programId,
            name = request.name,
            excludeId = editionId,
        )

        validateDateRange(
            startDate = request.startDate,
            endDate = request.endDate,
        )

        validateCapacity(request.maxCapacity)

        validateCapacityAgainstCurrentEnrollment(
            maxCapacity = request.maxCapacity,
            currentEnrollment = edition.currentEnrollment,
        )
    }

    fun validateStatusTransition(
        edition: ProgramEdition,
        newStatus: ProgramEditionStatus,
    ) {
        val currentStatus = edition.status

        val valid = when (currentStatus) {
            ProgramEditionStatus.DRAFT ->
                newStatus == ProgramEditionStatus.ACTIVE ||
                        newStatus == ProgramEditionStatus.CLOSED

            ProgramEditionStatus.ACTIVE ->
                newStatus == ProgramEditionStatus.SUSPENDED ||
                        newStatus == ProgramEditionStatus.CLOSED

            ProgramEditionStatus.SUSPENDED ->
                newStatus == ProgramEditionStatus.ACTIVE ||
                        newStatus == ProgramEditionStatus.CLOSED

            ProgramEditionStatus.CLOSED ->
                false
        }

        if (!valid) {
            throw ProgramEditionErrors.invalidStatusTransition(
                currentStatus = currentStatus,
                newStatus = newStatus,
            )
        }
    }

    fun validateDelete(edition: ProgramEdition) {
        if (edition.status == ProgramEditionStatus.CLOSED) {
            throw ProgramEditionErrors.closedEditionCannotBeDeleted()
        }

        val editionId = requireNotNull(edition.id)

        if (
            programBenefitRepository.existsByProgramEditionId(editionId) ||
            programRequirementRepository.existsByProgramEditionId(editionId)
        ) {
            throw ProgramEditionErrors.hasConfiguration(editionId)
        }

        if (edition.currentEnrollment > 0) {
            throw ProgramEditionErrors.hasEnrollments(
                id = editionId,
                currentEnrollment = edition.currentEnrollment,
            )
        }
    }

    fun validateCanModify(edition: ProgramEdition) {
        if (edition.status == ProgramEditionStatus.CLOSED) {
            throw ProgramEditionErrors.closedEditionCannotBeModified()
        }
    }

    private fun validateUniqueName(
        programId: UUID,
        name: String,
        excludeId: UUID? = null,
    ) {
        val trimmedName = name.trim()
        val normalizedName = trimmedName.lowercase()

        val exists = if (excludeId == null) {
            programEditionRepository.existsByProgramIdAndNormalizedName(
                programId = programId,
                normalizedName = normalizedName,
            )
        } else {
            programEditionRepository.existsByProgramIdAndNormalizedNameAndIdNot(
                programId = programId,
                normalizedName = normalizedName,
                id = excludeId,
            )
        }

        if (exists) {
            throw ProgramEditionErrors.nameAlreadyExists(
                programId = programId,
                name = trimmedName,
            )
        }
    }

    private fun validateDateRange(
        startDate: LocalDate,
        endDate: LocalDate,
    ) {
        if (endDate.isBefore(startDate)) {
            throw ProgramEditionErrors.invalidDateRange()
        }
    }

    private fun validateCapacity(maxCapacity: Int) {
        if (maxCapacity <= 0) {
            throw ProgramEditionErrors.invalidCapacity()
        }
    }

    private fun validateCapacityAgainstCurrentEnrollment(
        maxCapacity: Int,
        currentEnrollment: Int,
    ) {
        if (maxCapacity < currentEnrollment) {
            throw ProgramEditionErrors.capacityBelowCurrentEnrollment(
                currentEnrollment = currentEnrollment,
            )
        }
    }
}
