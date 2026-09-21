package com.uade.dda2.server.feature.program.service

import com.uade.dda2.server.feature.application.repository.ApplicationRepository
import com.uade.dda2.server.feature.auth.service.CurrentUserService
import com.uade.dda2.server.feature.log.entity.LogAction
import com.uade.dda2.server.feature.log.entity.LogEntityType
import com.uade.dda2.server.feature.log.service.LogService
import com.uade.dda2.server.feature.program.dto.admin.request.CreateProgramDocumentRequirementRequest
import com.uade.dda2.server.feature.program.dto.admin.request.UpdateProgramDocumentRequirementRequest
import com.uade.dda2.server.feature.program.dto.admin.response.ProgramDocumentRequirementResponse
import com.uade.dda2.server.feature.program.entity.ProgramDocumentRequirement
import com.uade.dda2.server.feature.program.entity.ProgramEdition
import com.uade.dda2.server.feature.program.error.ProgramDocumentRequirementErrors
import com.uade.dda2.server.feature.program.error.ProgramEditionErrors
import com.uade.dda2.server.feature.program.mapper.toAuditSnapshot
import com.uade.dda2.server.feature.program.mapper.toDocumentRequirementResponse
import com.uade.dda2.server.feature.program.mapper.toEntity
import com.uade.dda2.server.feature.program.mapper.updateFrom
import com.uade.dda2.server.feature.program.repository.ProgramDocumentRequirementRepository
import com.uade.dda2.server.feature.program.repository.ProgramEditionRepository
import com.uade.dda2.server.feature.program.validator.AdminProgramEditionValidator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.json.JsonMapper
import java.util.UUID

@Service
class AdminProgramDocumentRequirementService(
    private val editions: ProgramEditionRepository,
    private val requirements: ProgramDocumentRequirementRepository,
    private val applications: ApplicationRepository,
    private val editionValidator: AdminProgramEditionValidator,
    private val currentUserService: CurrentUserService,
    private val logService: LogService,
    private val jsonMapper: JsonMapper,
) {
    @Transactional(readOnly = true)
    fun list(editionId: UUID): List<ProgramDocumentRequirementResponse> {
        findEdition(editionId)
        return requirements.findAllByProgramEditionIdOrderByNameAsc(editionId).map { it.toDocumentRequirementResponse() }
    }

    @Transactional(readOnly = true)
    fun get(editionId: UUID, requirementId: UUID): ProgramDocumentRequirementResponse =
        findRequirement(editionId, requirementId).toDocumentRequirementResponse()

    @Transactional
    fun create(editionId: UUID, request: CreateProgramDocumentRequirementRequest): ProgramDocumentRequirementResponse {
        val edition = findEditionForUpdate(editionId)
        validateMutable(edition)
        val code = request.code.trim().uppercase()
        if (requirements.existsByProgramEditionIdAndCode(editionId, code)) {
            throw ProgramDocumentRequirementErrors.duplicateCode(code)
        }
        val saved = requirements.save(request.toEntity(edition))
        logService.record(
            user = currentUserService.userReference(),
            action = LogAction.CREATE,
            entityType = LogEntityType.PROGRAM_DOCUMENT_REQUIREMENT,
            entityId = requireNotNull(saved.id).toString(),
            newValues = json(saved.toAuditSnapshot()),
        )
        return saved.toDocumentRequirementResponse()
    }

    @Transactional
    fun update(editionId: UUID, requirementId: UUID, request: UpdateProgramDocumentRequirementRequest): ProgramDocumentRequirementResponse {
        val edition = findEditionForUpdate(editionId)
        validateMutable(edition)
        val requirement = findRequirement(editionId, requirementId)
        val code = request.code.trim().uppercase()
        if (requirements.existsByProgramEditionIdAndCodeAndIdNot(editionId, code, requirementId)) {
            throw ProgramDocumentRequirementErrors.duplicateCode(code)
        }
        val oldValues = json(requirement.toAuditSnapshot())
        requirement.updateFrom(request)
        val saved = requirements.save(requirement)
        logService.record(
            user = currentUserService.userReference(),
            action = LogAction.UPDATE,
            entityType = LogEntityType.PROGRAM_DOCUMENT_REQUIREMENT,
            entityId = requireNotNull(saved.id).toString(),
            oldValues = oldValues,
            newValues = json(saved.toAuditSnapshot()),
        )
        return saved.toDocumentRequirementResponse()
    }

    @Transactional
    fun delete(editionId: UUID, requirementId: UUID) {
        val edition = findEditionForUpdate(editionId)
        validateMutable(edition)
        val requirement = findRequirement(editionId, requirementId)
        val oldValues = json(requirement.toAuditSnapshot())
        requirements.delete(requirement)
        logService.record(
            user = currentUserService.userReference(),
            action = LogAction.DELETE,
            entityType = LogEntityType.PROGRAM_DOCUMENT_REQUIREMENT,
            entityId = requirementId.toString(),
            oldValues = oldValues,
        )
    }

    private fun validateMutable(edition: ProgramEdition) {
        editionValidator.validateCanModify(edition)
        if (applications.existsByProgramEditionId(requireNotNull(edition.id))) {
            throw ProgramDocumentRequirementErrors.catalogLocked()
        }
    }

    private fun findEdition(id: UUID): ProgramEdition = editions.findById(id).orElseThrow { ProgramEditionErrors.notFound(id) }
    private fun findEditionForUpdate(id: UUID): ProgramEdition = editions.findByIdForUpdate(id) ?: throw ProgramEditionErrors.notFound(id)
    private fun findRequirement(editionId: UUID, requirementId: UUID): ProgramDocumentRequirement =
        requirements.findByIdAndProgramEditionId(requirementId, editionId) ?: throw ProgramDocumentRequirementErrors.notFound()

    private fun json(value: Any): String =
        requireNotNull(jsonMapper.writeValueAsString(value))
}
