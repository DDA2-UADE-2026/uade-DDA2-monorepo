package com.uade.dda2.server.feature.program.service

import com.uade.dda2.server.feature.auth.service.CurrentUserService
import com.uade.dda2.server.feature.log.entity.LogAction
import com.uade.dda2.server.feature.log.entity.LogEntityType
import com.uade.dda2.server.feature.log.service.LogService
import com.uade.dda2.server.feature.program.dto.admin.request.CreateProgramBenefitRequest
import com.uade.dda2.server.feature.program.dto.admin.request.UpdateProgramBenefitRequest
import com.uade.dda2.server.feature.program.dto.admin.response.ProgramBenefitResponse
import com.uade.dda2.server.feature.program.entity.ProgramBenefit
import com.uade.dda2.server.feature.program.entity.ProgramEdition
import com.uade.dda2.server.feature.program.error.ProgramBenefitErrors
import com.uade.dda2.server.feature.program.error.ProgramEditionErrors
import com.uade.dda2.server.feature.program.mapper.toAuditSnapshot
import com.uade.dda2.server.feature.program.mapper.toEntity
import com.uade.dda2.server.feature.program.mapper.toResponse
import com.uade.dda2.server.feature.program.mapper.updateFrom
import com.uade.dda2.server.feature.program.repository.ProgramBenefitRepository
import com.uade.dda2.server.feature.program.repository.ProgramEditionRepository
import com.uade.dda2.server.feature.program.validator.AdminProgramBenefitValidator
import com.uade.dda2.server.feature.program.validator.AdminProgramEditionValidator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.json.JsonMapper
import java.util.UUID

@Service
class AdminProgramBenefitService(
    private val programEditionRepository: ProgramEditionRepository,
    private val programBenefitRepository: ProgramBenefitRepository,
    private val adminProgramBenefitValidator: AdminProgramBenefitValidator,
    private val adminProgramEditionValidator: AdminProgramEditionValidator,
    private val currentUserService: CurrentUserService,
    private val logService: LogService,
    private val jsonMapper: JsonMapper,
) {

    @Transactional(readOnly = true)
    fun list(
        editionId: UUID,
    ): List<ProgramBenefitResponse> {
        findEdition(editionId)

        return programBenefitRepository
            .findAllByProgramEditionId(editionId)
            .map { it.toResponse() }
    }

    @Transactional(readOnly = true)
    fun get(
        editionId: UUID,
        benefitId: UUID,
    ): ProgramBenefitResponse =
        findBenefit(
            editionId = editionId,
            benefitId = benefitId,
        ).toResponse()

    @Transactional
    fun create(
        editionId: UUID,
        request: CreateProgramBenefitRequest,
    ): ProgramBenefitResponse {
        val edition = findEdition(editionId)

        adminProgramEditionValidator.validateCanModify(edition)
        adminProgramBenefitValidator.validateCreate(request)

        val benefit = request.toEntity(
            programEdition = edition,
        )

        val saved = programBenefitRepository.save(benefit)
        logService.record(
            user = currentUserService.userReference(),
            action = LogAction.CREATE,
            entityType = LogEntityType.PROGRAM_BENEFIT,
            entityId = requireNotNull(saved.id).toString(),
            newValues = json(saved.toAuditSnapshot()),
        )

        return saved.toResponse()
    }

    @Transactional
    fun update(
        editionId: UUID,
        benefitId: UUID,
        request: UpdateProgramBenefitRequest,
    ): ProgramBenefitResponse {
        val benefit = findBenefit(
            editionId = editionId,
            benefitId = benefitId,
        )

        adminProgramEditionValidator.validateCanModify(benefit.programEdition)
        adminProgramBenefitValidator.validateUpdate(request)

        val oldValues = json(benefit.toAuditSnapshot())
        benefit.updateFrom(request)

        val saved = programBenefitRepository.save(benefit)
        logService.record(
            user = currentUserService.userReference(),
            action = LogAction.UPDATE,
            entityType = LogEntityType.PROGRAM_BENEFIT,
            entityId = requireNotNull(saved.id).toString(),
            oldValues = oldValues,
            newValues = json(saved.toAuditSnapshot()),
        )

        return saved.toResponse()
    }

    @Transactional
    fun delete(
        editionId: UUID,
        benefitId: UUID,
    ) {
        val benefit = findBenefit(
            editionId = editionId,
            benefitId = benefitId,
        )

        adminProgramEditionValidator.validateCanModify(benefit.programEdition)

        val oldValues = json(benefit.toAuditSnapshot())
        programBenefitRepository.delete(benefit)

        logService.record(
            user = currentUserService.userReference(),
            action = LogAction.DELETE,
            entityType = LogEntityType.PROGRAM_BENEFIT,
            entityId = benefitId.toString(),
            oldValues = oldValues,
        )
    }

    private fun findEdition(id: UUID): ProgramEdition =
        programEditionRepository
            .findById(id)
            .orElseThrow {
                ProgramEditionErrors.notFound(id)
            }

    private fun findBenefit(
        editionId: UUID,
        benefitId: UUID,
    ): ProgramBenefit =
        programBenefitRepository
            .findByIdAndProgramEditionId(
                id = benefitId,
                programEditionId = editionId,
            )
            ?: throw ProgramBenefitErrors.notFound(
                id = benefitId,
                programEditionId = editionId,
            )

    private fun json(value: Any): String =
        requireNotNull(jsonMapper.writeValueAsString(value))
}
