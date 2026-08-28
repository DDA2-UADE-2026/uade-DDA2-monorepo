package com.uade.dda2.server.feature.enrollmentperiod.service

import com.uade.dda2.server.feature.auth.service.CurrentUserService
import com.uade.dda2.server.feature.enrollmentperiod.dto.request.CreateEnrollmentPeriodRequest
import com.uade.dda2.server.feature.enrollmentperiod.dto.request.UpdateEnrollmentPeriodRequest
import com.uade.dda2.server.feature.enrollmentperiod.dto.response.EnrollmentPeriodListResponse
import com.uade.dda2.server.feature.enrollmentperiod.dto.response.EnrollmentPeriodResponse
import com.uade.dda2.server.feature.enrollmentperiod.entity.EnrollmentPeriod
import com.uade.dda2.server.feature.enrollmentperiod.entity.EnrollmentPeriodStatus
import com.uade.dda2.server.feature.enrollmentperiod.error.EnrollmentPeriodErrors
import com.uade.dda2.server.feature.enrollmentperiod.mapper.toEntity
import com.uade.dda2.server.feature.enrollmentperiod.mapper.toAuditSnapshot
import com.uade.dda2.server.feature.enrollmentperiod.mapper.toListResponse
import com.uade.dda2.server.feature.enrollmentperiod.mapper.toResponse
import com.uade.dda2.server.feature.enrollmentperiod.mapper.updateFrom
import com.uade.dda2.server.feature.enrollmentperiod.repository.EnrollmentPeriodRepository
import com.uade.dda2.server.feature.enrollmentperiod.validator.EnrollmentPeriodValidator
import com.uade.dda2.server.feature.log.entity.LogAction
import com.uade.dda2.server.feature.log.entity.LogEntityType
import com.uade.dda2.server.feature.log.service.LogService
import com.uade.dda2.server.feature.program.entity.ProgramEdition
import com.uade.dda2.server.feature.program.error.ProgramEditionErrors
import com.uade.dda2.server.feature.program.error.ProgramErrors
import com.uade.dda2.server.feature.program.repository.ProgramEditionRepository
import com.uade.dda2.server.feature.program.repository.ProgramRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.json.JsonMapper
import java.time.LocalDate
import java.util.UUID

@Service
class AdminEnrollmentPeriodService(
    private val programRepository: ProgramRepository,
    private val programEditionRepository: ProgramEditionRepository,
    private val enrollmentPeriodRepository: EnrollmentPeriodRepository,
    private val enrollmentPeriodValidator: EnrollmentPeriodValidator,
    private val currentUserService: CurrentUserService,
    private val logService: LogService,
    private val jsonMapper: JsonMapper,
) {

    @Transactional(readOnly = true)
    fun list(
        programId: UUID,
        programEditionId: UUID,
        page: Int,
        size: Int,
    ): EnrollmentPeriodListResponse {
        findProgram(programId)
        findEdition(
            programId = programId,
            programEditionId = programEditionId,
        )

        return enrollmentPeriodRepository
            .findAllByProgramEditionIdOrderByOpenDateDesc(
                programEditionId = programEditionId,
                pageable = PageRequest.of(page, size),
            )
            .toListResponse()
    }

    @Transactional(readOnly = true)
    fun get(
        programId: UUID,
        programEditionId: UUID,
        enrollmentPeriodId: UUID,
    ): EnrollmentPeriodResponse {
        findProgram(programId)
        findEdition(
            programId = programId,
            programEditionId = programEditionId,
        )

        return findEnrollmentPeriod(
            programEditionId = programEditionId,
            enrollmentPeriodId = enrollmentPeriodId,
        ).toResponse()
    }

    @Transactional
    fun create(
        programId: UUID,
        programEditionId: UUID,
        request: CreateEnrollmentPeriodRequest,
    ): EnrollmentPeriodResponse {
        findProgram(programId)
        val programEdition = findEdition(
            programId = programId,
            programEditionId = programEditionId,
            forUpdate = true,
        )

        enrollmentPeriodValidator.validateCreate(
            programEdition = programEdition,
            request = request,
        )

        val enrollmentPeriod = enrollmentPeriodRepository.saveAndFlush(
            request.toEntity(programEdition),
        )
        recordCreate(enrollmentPeriod)

        return enrollmentPeriod.toResponse()
    }

    @Transactional
    fun update(
        programId: UUID,
        programEditionId: UUID,
        enrollmentPeriodId: UUID,
        request: UpdateEnrollmentPeriodRequest,
    ): EnrollmentPeriodResponse {
        findProgram(programId)
        findEdition(
            programId = programId,
            programEditionId = programEditionId,
            forUpdate = true,
        )
        val enrollmentPeriod = findEnrollmentPeriod(
            programEditionId = programEditionId,
            enrollmentPeriodId = enrollmentPeriodId,
            forUpdate = true,
        )

        enrollmentPeriodValidator.validateUpdate(
            enrollmentPeriod = enrollmentPeriod,
            request = request,
        )

        val oldValues = json(enrollmentPeriod.toAuditSnapshot())
        enrollmentPeriod.updateFrom(request)
        enrollmentPeriodRepository.saveAndFlush(enrollmentPeriod)
        recordUpdate(
            enrollmentPeriod = enrollmentPeriod,
            oldValues = oldValues,
        )

        return enrollmentPeriod.toResponse()
    }

    @Transactional
    fun open(
        programId: UUID,
        programEditionId: UUID,
        enrollmentPeriodId: UUID,
    ): EnrollmentPeriodResponse =
        changeStatus(
            programId = programId,
            programEditionId = programEditionId,
            enrollmentPeriodId = enrollmentPeriodId,
            newStatus = EnrollmentPeriodStatus.OPEN,
        ) { enrollmentPeriod ->
            enrollmentPeriodValidator.validateOpen(
                enrollmentPeriod = enrollmentPeriod,
                currentDate = LocalDate.now(),
            )
        }

    @Transactional
    fun suspend(
        programId: UUID,
        programEditionId: UUID,
        enrollmentPeriodId: UUID,
    ): EnrollmentPeriodResponse =
        changeStatus(
            programId = programId,
            programEditionId = programEditionId,
            enrollmentPeriodId = enrollmentPeriodId,
            newStatus = EnrollmentPeriodStatus.SUSPENDED,
            validate = enrollmentPeriodValidator::validateSuspend,
        )

    @Transactional
    fun reopen(
        programId: UUID,
        programEditionId: UUID,
        enrollmentPeriodId: UUID,
    ): EnrollmentPeriodResponse =
        changeStatus(
            programId = programId,
            programEditionId = programEditionId,
            enrollmentPeriodId = enrollmentPeriodId,
            newStatus = EnrollmentPeriodStatus.OPEN,
        ) { enrollmentPeriod ->
            enrollmentPeriodValidator.validateReopen(
                enrollmentPeriod = enrollmentPeriod,
                currentDate = LocalDate.now(),
            )
        }

    @Transactional
    fun close(
        programId: UUID,
        programEditionId: UUID,
        enrollmentPeriodId: UUID,
    ): EnrollmentPeriodResponse =
        changeStatus(
            programId = programId,
            programEditionId = programEditionId,
            enrollmentPeriodId = enrollmentPeriodId,
            newStatus = EnrollmentPeriodStatus.CLOSED,
            validate = enrollmentPeriodValidator::validateClose,
        )

    private fun changeStatus(
        programId: UUID,
        programEditionId: UUID,
        enrollmentPeriodId: UUID,
        newStatus: EnrollmentPeriodStatus,
        validate: (EnrollmentPeriod) -> Unit,
    ): EnrollmentPeriodResponse {
        findProgram(programId)
        findEdition(
            programId = programId,
            programEditionId = programEditionId,
            forUpdate = true,
        )
        val enrollmentPeriod = findEnrollmentPeriod(
            programEditionId = programEditionId,
            enrollmentPeriodId = enrollmentPeriodId,
            forUpdate = true,
        )

        validate(enrollmentPeriod)

        val oldValues = json(enrollmentPeriod.toAuditSnapshot())
        enrollmentPeriod.status = newStatus
        enrollmentPeriodRepository.saveAndFlush(enrollmentPeriod)
        recordUpdate(
            enrollmentPeriod = enrollmentPeriod,
            oldValues = oldValues,
        )

        return enrollmentPeriod.toResponse()
    }

    private fun findProgram(id: UUID) =
        programRepository
            .findById(id)
            .orElseThrow {
                ProgramErrors.notFound(id)
            }

    private fun findEdition(
        programId: UUID,
        programEditionId: UUID,
        forUpdate: Boolean = false,
    ): ProgramEdition {
        val programEdition = if (forUpdate) {
            programEditionRepository.findByIdForUpdate(programEditionId)
        } else {
            programEditionRepository.findById(programEditionId).orElse(null)
        } ?: throw ProgramEditionErrors.notFound(programEditionId)

        enrollmentPeriodValidator.validateEditionBelongsToProgram(
            programId = programId,
            programEdition = programEdition,
        )

        return programEdition
    }

    private fun findEnrollmentPeriod(
        programEditionId: UUID,
        enrollmentPeriodId: UUID,
        forUpdate: Boolean = false,
    ): EnrollmentPeriod {
        val enrollmentPeriod = if (forUpdate) {
            enrollmentPeriodRepository.findByIdForUpdate(enrollmentPeriodId)
        } else {
            enrollmentPeriodRepository.findById(enrollmentPeriodId).orElse(null)
        } ?: throw EnrollmentPeriodErrors.notFound(enrollmentPeriodId)

        enrollmentPeriodValidator.validatePeriodBelongsToEdition(
            programEditionId = programEditionId,
            enrollmentPeriod = enrollmentPeriod,
        )

        return enrollmentPeriod
    }

    private fun recordCreate(enrollmentPeriod: EnrollmentPeriod) {
        logService.record(
            user = currentUserService.userReference(),
            action = LogAction.CREATE,
            entityType = LogEntityType.ENROLLMENT_PERIOD,
            entityId = requireNotNull(enrollmentPeriod.id).toString(),
            newValues = json(enrollmentPeriod.toAuditSnapshot()),
        )
    }

    private fun recordUpdate(
        enrollmentPeriod: EnrollmentPeriod,
        oldValues: String,
    ) {
        logService.record(
            user = currentUserService.userReference(),
            action = LogAction.UPDATE,
            entityType = LogEntityType.ENROLLMENT_PERIOD,
            entityId = requireNotNull(enrollmentPeriod.id).toString(),
            oldValues = oldValues,
            newValues = json(enrollmentPeriod.toAuditSnapshot()),
        )
    }

    private fun json(value: Any): String =
        requireNotNull(jsonMapper.writeValueAsString(value))
}
