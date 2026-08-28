package com.uade.dda2.server.feature.enrollmentperiod.validator

import com.uade.dda2.server.feature.enrollmentperiod.dto.request.CreateEnrollmentPeriodRequest
import com.uade.dda2.server.feature.enrollmentperiod.dto.request.UpdateEnrollmentPeriodRequest
import com.uade.dda2.server.feature.enrollmentperiod.entity.EnrollmentPeriod
import com.uade.dda2.server.feature.enrollmentperiod.entity.EnrollmentPeriodStatus
import com.uade.dda2.server.feature.enrollmentperiod.error.EnrollmentPeriodErrors
import com.uade.dda2.server.feature.enrollmentperiod.repository.EnrollmentPeriodRepository
import com.uade.dda2.server.feature.program.entity.ProgramEdition
import com.uade.dda2.server.feature.program.entity.enums.ProgramEditionStatus
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.UUID

@Component
class EnrollmentPeriodValidator(
    private val enrollmentPeriodRepository: EnrollmentPeriodRepository,
) {

    fun validateCreate(
        programEdition: ProgramEdition,
        request: CreateEnrollmentPeriodRequest,
    ) {
        validateDates(
            programEdition = programEdition,
            openDate = request.openDate,
            closeDate = request.closeDate,
        )

        val programEditionId = requireNotNull(programEdition.id)
        if (
            enrollmentPeriodRepository.existsOverlapping(
                programEditionId = programEditionId,
                openDate = request.openDate,
                closeDate = request.closeDate,
            )
        ) {
            throw EnrollmentPeriodErrors.overlapsExistingPeriod()
        }
    }

    fun validateUpdate(
        enrollmentPeriod: EnrollmentPeriod,
        request: UpdateEnrollmentPeriodRequest,
    ) {
        if (enrollmentPeriod.status == EnrollmentPeriodStatus.CLOSED) {
            throw EnrollmentPeriodErrors.closedPeriodCannotBeModified()
        }

        validateDates(
            programEdition = enrollmentPeriod.programEdition,
            openDate = request.openDate,
            closeDate = request.closeDate,
        )

        if (
            enrollmentPeriodRepository.existsOverlappingExcluding(
                programEditionId = requireNotNull(enrollmentPeriod.programEdition.id),
                excludedId = requireNotNull(enrollmentPeriod.id),
                openDate = request.openDate,
                closeDate = request.closeDate,
            )
        ) {
            throw EnrollmentPeriodErrors.overlapsExistingPeriod()
        }
    }

    fun validateOpen(
        enrollmentPeriod: EnrollmentPeriod,
        currentDate: LocalDate,
    ) {
        validateTransition(
            enrollmentPeriod = enrollmentPeriod,
            expectedCurrentStatus = EnrollmentPeriodStatus.SCHEDULED,
            newStatus = EnrollmentPeriodStatus.OPEN,
        )
        validateEditionIsActive(enrollmentPeriod.programEdition)
        validateCurrentDateWithinPeriod(
            enrollmentPeriod = enrollmentPeriod,
            currentDate = currentDate,
        )
        validateNoOtherOpenPeriod(enrollmentPeriod)
    }

    fun validateSuspend(enrollmentPeriod: EnrollmentPeriod) {
        validateTransition(
            enrollmentPeriod = enrollmentPeriod,
            expectedCurrentStatus = EnrollmentPeriodStatus.OPEN,
            newStatus = EnrollmentPeriodStatus.SUSPENDED,
        )
    }

    fun validateReopen(
        enrollmentPeriod: EnrollmentPeriod,
        currentDate: LocalDate,
    ) {
        validateTransition(
            enrollmentPeriod = enrollmentPeriod,
            expectedCurrentStatus = EnrollmentPeriodStatus.SUSPENDED,
            newStatus = EnrollmentPeriodStatus.OPEN,
        )

        if (currentDate.isAfter(enrollmentPeriod.closeDate)) {
            throw EnrollmentPeriodErrors.cannotReopenAfterCloseDate(enrollmentPeriod.closeDate)
        }

        validateEditionIsActive(enrollmentPeriod.programEdition)
        validateCurrentDateWithinPeriod(
            enrollmentPeriod = enrollmentPeriod,
            currentDate = currentDate,
        )
        validateNoOtherOpenPeriod(enrollmentPeriod)
    }

    fun validateClose(enrollmentPeriod: EnrollmentPeriod) {
        val canClose = enrollmentPeriod.status == EnrollmentPeriodStatus.OPEN ||
                enrollmentPeriod.status == EnrollmentPeriodStatus.SUSPENDED

        if (!canClose) {
            throw EnrollmentPeriodErrors.invalidStatusTransition(
                currentStatus = enrollmentPeriod.status,
                newStatus = EnrollmentPeriodStatus.CLOSED,
            )
        }
    }

    fun validateCanReceiveApplication(
        programEditionId: UUID,
        enrollmentPeriod: EnrollmentPeriod,
        currentDate: LocalDate,
    ) {
        validatePeriodBelongsToEdition(
            programEditionId = programEditionId,
            enrollmentPeriod = enrollmentPeriod,
        )

        if (enrollmentPeriod.status != EnrollmentPeriodStatus.OPEN) {
            throw EnrollmentPeriodErrors.notOpen(enrollmentPeriod.status)
        }

        validateCurrentDateWithinPeriod(
            enrollmentPeriod = enrollmentPeriod,
            currentDate = currentDate,
        )
    }

    fun validateEditionBelongsToProgram(
        programId: UUID,
        programEdition: ProgramEdition,
    ) {
        if (programEdition.program.id != programId) {
            throw EnrollmentPeriodErrors.programEditionMismatch(
                programId = programId,
                programEditionId = requireNotNull(programEdition.id),
            )
        }
    }

    fun validatePeriodBelongsToEdition(
        programEditionId: UUID,
        enrollmentPeriod: EnrollmentPeriod,
    ) {
        if (enrollmentPeriod.programEdition.id != programEditionId) {
            throw EnrollmentPeriodErrors.periodEditionMismatch(
                enrollmentPeriodId = requireNotNull(enrollmentPeriod.id),
                programEditionId = programEditionId,
            )
        }
    }

    private fun validateDates(
        programEdition: ProgramEdition,
        openDate: LocalDate,
        closeDate: LocalDate,
    ) {
        if (closeDate.isBefore(openDate)) {
            throw EnrollmentPeriodErrors.invalidDateRange()
        }

        if (
            openDate.isBefore(programEdition.startDate) ||
            closeDate.isAfter(programEdition.endDate)
        ) {
            throw EnrollmentPeriodErrors.outsideEditionDateRange(
                editionStartDate = programEdition.startDate,
                editionEndDate = programEdition.endDate,
            )
        }
    }

    private fun validateTransition(
        enrollmentPeriod: EnrollmentPeriod,
        expectedCurrentStatus: EnrollmentPeriodStatus,
        newStatus: EnrollmentPeriodStatus,
    ) {
        if (enrollmentPeriod.status != expectedCurrentStatus) {
            throw EnrollmentPeriodErrors.invalidStatusTransition(
                currentStatus = enrollmentPeriod.status,
                newStatus = newStatus,
            )
        }
    }

    private fun validateEditionIsActive(programEdition: ProgramEdition) {
        if (programEdition.status != ProgramEditionStatus.ACTIVE) {
            throw EnrollmentPeriodErrors.editionNotActive(programEdition.status)
        }
    }

    private fun validateCurrentDateWithinPeriod(
        enrollmentPeriod: EnrollmentPeriod,
        currentDate: LocalDate,
    ) {
        if (
            currentDate.isBefore(enrollmentPeriod.openDate) ||
            currentDate.isAfter(enrollmentPeriod.closeDate)
        ) {
            throw EnrollmentPeriodErrors.outsideActiveDateRange(
                currentDate = currentDate,
                openDate = enrollmentPeriod.openDate,
                closeDate = enrollmentPeriod.closeDate,
            )
        }
    }

    private fun validateNoOtherOpenPeriod(enrollmentPeriod: EnrollmentPeriod) {
        if (
            enrollmentPeriodRepository.existsByProgramEditionIdAndStatusAndIdNot(
                programEditionId = requireNotNull(enrollmentPeriod.programEdition.id),
                status = EnrollmentPeriodStatus.OPEN,
                id = requireNotNull(enrollmentPeriod.id),
            )
        ) {
            throw EnrollmentPeriodErrors.alreadyOpenForEdition()
        }
    }
}
