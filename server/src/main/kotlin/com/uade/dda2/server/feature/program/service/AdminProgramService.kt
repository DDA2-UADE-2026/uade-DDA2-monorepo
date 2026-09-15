package com.uade.dda2.server.feature.program.service

import com.uade.dda2.server.feature.auth.service.CurrentUserService
import com.uade.dda2.server.feature.log.entity.LogAction
import com.uade.dda2.server.feature.log.entity.LogEntityType
import com.uade.dda2.server.feature.log.service.LogService
import com.uade.dda2.server.feature.program.dto.admin.request.CreateProgramRequest
import com.uade.dda2.server.feature.program.dto.admin.request.UpdateProgramRequest
import com.uade.dda2.server.feature.program.dto.admin.response.ProgramListResponse
import com.uade.dda2.server.feature.program.dto.admin.response.ProgramOptionResponse
import com.uade.dda2.server.feature.program.dto.admin.response.ProgramResponse
import com.uade.dda2.server.feature.program.entity.Program
import com.uade.dda2.server.feature.program.entity.enums.ProgramEditionStatus
import com.uade.dda2.server.feature.program.error.ProgramErrors
import com.uade.dda2.server.feature.program.mapper.toAuditSnapshot
import com.uade.dda2.server.feature.program.mapper.toEntity
import com.uade.dda2.server.feature.program.mapper.toListResponse
import com.uade.dda2.server.feature.program.mapper.toOptionResponse
import com.uade.dda2.server.feature.program.mapper.toResponse
import com.uade.dda2.server.feature.program.mapper.updateFrom
import com.uade.dda2.server.feature.program.repository.ProgramEditionRepository
import com.uade.dda2.server.feature.program.repository.ProgramImageRepository
import com.uade.dda2.server.feature.program.repository.ProgramRepository
import com.uade.dda2.server.feature.program.validator.AdminProgramValidator
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.json.JsonMapper
import java.util.*

@Service
class AdminProgramService(
    private val programRepository: ProgramRepository,
    private val programEditionRepository: ProgramEditionRepository,
    private val programImageRepository: ProgramImageRepository,
    private val adminProgramValidator: AdminProgramValidator,
    private val currentUserService: CurrentUserService,
    private val logService: LogService,
    private val jsonMapper: JsonMapper,
) {

    @Transactional(readOnly = true)
    fun list(
        page: Int,
        size: Int,
    ): ProgramListResponse {
        val pageable = PageRequest.of(page, size)

        val programs = programRepository.findAllByOrderByNameAsc(pageable)
        val programIds = programs.content.map { requireNotNull(it.id) }
        val activeProgramIds = if (programIds.isEmpty()) {
            emptySet()
        } else {
            programEditionRepository.findProgramIdsByStatus(
                programIds = programIds,
                status = ProgramEditionStatus.ACTIVE,
            ).toSet()
        }
        val imageIdsByProgram = findImageIds(programIds)

        return programs.toListResponse(
            activeProgramIds = activeProgramIds,
            imageIdsByProgram = imageIdsByProgram,
        )
    }

    @Transactional(readOnly = true)
    fun get(id: UUID): ProgramResponse =
        findProgram(id).toResponse(programImageRepository.findImageIdByProgramId(id))

    @Transactional(readOnly = true)
    fun options(): List<ProgramOptionResponse> {
        val programs = programRepository.findAllByOrderByNameAsc()
        val imageIdsByProgram = findImageIds(programs.map { requireNotNull(it.id) })

        return programs.map { program ->
            val programId = requireNotNull(program.id)
            program.toOptionResponse(imageIdsByProgram[programId])
        }
    }

    @Transactional
    fun create(request: CreateProgramRequest): ProgramResponse {
        adminProgramValidator.validateCreate(request)

        val program = request.toEntity(
            createdBy = currentUserService.userReference(),
        )

        val saved = try {
            programRepository.saveAndFlush(program)
        } catch (_: DataIntegrityViolationException) {
            throw ProgramErrors.nameAlreadyExists(program.name)
        }

        logService.record(
            user = currentUserService.userReference(),
            action = LogAction.CREATE,
            entityType = LogEntityType.PROGRAM,
            entityId = requireNotNull(saved.id).toString(),
            newValues = json(saved.toAuditSnapshot()),
        )

        return saved.toResponse(imageId = null)
    }

    @Transactional
    fun update(
        id: UUID,
        request: UpdateProgramRequest,
    ): ProgramResponse {
        val program = findProgram(id)

        adminProgramValidator.validateUpdate(
            program = program,
            request = request,
        )

        val oldValues = json(program.toAuditSnapshot())
        program.updateFrom(request)

        val saved = try {
            programRepository.saveAndFlush(program)
        } catch (_: DataIntegrityViolationException) {
            throw ProgramErrors.nameAlreadyExists(program.name)
        }

        logService.record(
            user = currentUserService.userReference(),
            action = LogAction.UPDATE,
            entityType = LogEntityType.PROGRAM,
            entityId = id.toString(),
            oldValues = oldValues,
            newValues = json(saved.toAuditSnapshot()),
        )

        return saved.toResponse(programImageRepository.findImageIdByProgramId(id))
    }

    @Transactional
    fun delete(id: UUID) {
        val program = findProgram(id)

        adminProgramValidator.validateDelete(program)

        val oldValues = json(program.toAuditSnapshot())

        programImageRepository.deleteByProgramId(id)
        programRepository.delete(program)

        logService.record(
            user = currentUserService.userReference(),
            action = LogAction.DELETE,
            entityType = LogEntityType.PROGRAM,
            entityId = id.toString(),
            oldValues = oldValues,
        )
    }

    private fun findProgram(id: UUID): Program =
        programRepository
            .findById(id)
            .orElseThrow {
                ProgramErrors.notFound(id)
            }

    private fun findImageIds(programIds: Collection<UUID>): Map<UUID, UUID> =
        if (programIds.isEmpty()) {
            emptyMap()
        } else {
            programImageRepository
                .findReferencesByProgramIdIn(programIds)
                .associate { it.programId to it.id }
        }

    private fun json(value: Any): String =
        requireNotNull(jsonMapper.writeValueAsString(value))
}
