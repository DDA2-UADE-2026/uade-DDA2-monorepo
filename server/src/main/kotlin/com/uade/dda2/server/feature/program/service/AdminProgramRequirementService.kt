package com.uade.dda2.server.feature.program.service

import com.uade.dda2.server.feature.auth.service.CurrentUserService
import com.uade.dda2.server.feature.log.entity.LogAction
import com.uade.dda2.server.feature.log.entity.LogEntityType
import com.uade.dda2.server.feature.log.service.LogService
import com.uade.dda2.server.feature.program.dto.admin.request.CreateProgramRequirementRequest
import com.uade.dda2.server.feature.program.dto.admin.request.UpdateProgramRequirementRequest
import com.uade.dda2.server.feature.program.dto.admin.response.ProgramRequirementResponse
import com.uade.dda2.server.feature.program.entity.ProgramEdition
import com.uade.dda2.server.feature.program.entity.ProgramRequirement
import com.uade.dda2.server.feature.program.error.ProgramEditionErrors
import com.uade.dda2.server.feature.program.error.ProgramRequirementErrors
import com.uade.dda2.server.feature.program.mapper.toAuditSnapshot
import com.uade.dda2.server.feature.program.mapper.toEntity
import com.uade.dda2.server.feature.program.mapper.toResponse
import com.uade.dda2.server.feature.program.mapper.updateFrom
import com.uade.dda2.server.feature.program.repository.ProgramEditionRepository
import com.uade.dda2.server.feature.program.repository.ProgramRequirementRepository
import com.uade.dda2.server.feature.program.validator.AdminProgramRequirementValidator
import com.uade.dda2.server.feature.program.validator.AdminProgramEditionValidator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.json.JsonMapper
import java.util.UUID

@Service
class AdminProgramRequirementService(
    private val programEditionRepository: ProgramEditionRepository,
    private val programRequirementRepository: ProgramRequirementRepository,
    private val adminProgramRequirementValidator: AdminProgramRequirementValidator,
    private val adminProgramEditionValidator: AdminProgramEditionValidator,
    private val currentUserService: CurrentUserService,
    private val logService: LogService,
    private val jsonMapper: JsonMapper,
) {

    @Transactional(readOnly = true)
    fun list(
        editionId: UUID,
    ): List<ProgramRequirementResponse> {
        findEdition(editionId)

        return programRequirementRepository
            .findAllByProgramEditionId(editionId)
            .map { it.toResponse() }
    }

    @Transactional(readOnly = true)
    fun get(
        editionId: UUID,
        requirementId: UUID,
    ): ProgramRequirementResponse =
        findRequirement(
            editionId = editionId,
            requirementId = requirementId,
        ).toResponse()

    @Transactional
    fun create(
        editionId: UUID,
        request: CreateProgramRequirementRequest,
    ): ProgramRequirementResponse {
        val edition = findEdition(editionId)

        adminProgramEditionValidator.validateCanModify(edition)
        adminProgramRequirementValidator.validateCreate(request)

        val requirement = request.toEntity(
            programEdition = edition,
        )

        val saved = programRequirementRepository.save(requirement)
        logService.record(
            user = currentUserService.userReference(),
            action = LogAction.CREATE,
            entityType = LogEntityType.PROGRAM_REQUIREMENT,
            entityId = requireNotNull(saved.id).toString(),
            newValues = json(saved.toAuditSnapshot()),
        )

        return saved.toResponse()
    }

    @Transactional
    fun update(
        editionId: UUID,
        requirementId: UUID,
        request: UpdateProgramRequirementRequest,
    ): ProgramRequirementResponse {
        val requirement = findRequirement(
            editionId = editionId,
            requirementId = requirementId,
        )

        adminProgramEditionValidator.validateCanModify(requirement.programEdition)
        adminProgramRequirementValidator.validateUpdate(request)

        val oldValues = json(requirement.toAuditSnapshot())
        requirement.updateFrom(request)

        val saved = programRequirementRepository.save(requirement)
        logService.record(
            user = currentUserService.userReference(),
            action = LogAction.UPDATE,
            entityType = LogEntityType.PROGRAM_REQUIREMENT,
            entityId = requireNotNull(saved.id).toString(),
            oldValues = oldValues,
            newValues = json(saved.toAuditSnapshot()),
        )

        return saved.toResponse()
    }

    @Transactional
    fun delete(
        editionId: UUID,
        requirementId: UUID,
    ) {
        val requirement = findRequirement(
            editionId = editionId,
            requirementId = requirementId,
        )

        adminProgramEditionValidator.validateCanModify(requirement.programEdition)

        val oldValues = json(requirement.toAuditSnapshot())
        programRequirementRepository.delete(requirement)

        logService.record(
            user = currentUserService.userReference(),
            action = LogAction.DELETE,
            entityType = LogEntityType.PROGRAM_REQUIREMENT,
            entityId = requirementId.toString(),
            oldValues = oldValues,
        )
    }

    private fun findEdition(id: UUID): ProgramEdition =
        programEditionRepository
            .findById(id)
            .orElseThrow {
                ProgramEditionErrors.notFound(id)
            }

    private fun findRequirement(
        editionId: UUID,
        requirementId: UUID,
    ): ProgramRequirement =
        programRequirementRepository
            .findByIdAndProgramEditionId(
                id = requirementId,
                programEditionId = editionId,
            )
            ?: throw ProgramRequirementErrors.notFound(
                id = requirementId,
                programEditionId = editionId,
            )

    private fun json(value: Any): String =
        requireNotNull(jsonMapper.writeValueAsString(value))
}
