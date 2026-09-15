package com.uade.dda2.server.feature.program.service

import com.uade.dda2.server.feature.auth.service.CurrentUserService
import com.uade.dda2.server.feature.log.entity.LogAction
import com.uade.dda2.server.feature.log.entity.LogEntityType
import com.uade.dda2.server.feature.log.service.LogService
import com.uade.dda2.server.feature.program.dto.admin.response.ProgramIncompatibilityResponse
import com.uade.dda2.server.feature.program.entity.Program
import com.uade.dda2.server.feature.program.entity.ProgramIncompatibility
import com.uade.dda2.server.feature.program.error.ProgramErrors
import com.uade.dda2.server.feature.program.error.ProgramIncompatibilityErrors
import com.uade.dda2.server.feature.program.mapper.toAuditSnapshot
import com.uade.dda2.server.feature.program.mapper.toProgramIncompatibility
import com.uade.dda2.server.feature.program.repository.ProgramIncompatibilityRepository
import com.uade.dda2.server.feature.program.repository.ProgramRepository
import com.uade.dda2.server.feature.program.validator.AdminProgramIncompatibilityValidator
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.json.JsonMapper
import java.util.UUID

@Service
class AdminProgramIncompatibilityService(
    private val programRepository: ProgramRepository,
    private val programIncompatibilityRepository: ProgramIncompatibilityRepository,
    private val adminProgramIncompatibilityValidator: AdminProgramIncompatibilityValidator,
    private val currentUserService: CurrentUserService,
    private val logService: LogService,
    private val jsonMapper: JsonMapper,
) {

    @Transactional(readOnly = true)
    fun list(
        programId: UUID,
    ): List<ProgramIncompatibilityResponse> {
        val program = findProgram(programId)

        return programIncompatibilityRepository
            .findAllByProgramId(programId)
            .map {
                it.toResponseFor(program)
            }
    }

    @Transactional
    fun create(
        programId: UUID,
        incompatibleProgramId: UUID,
    ): ProgramIncompatibilityResponse {
        val program = findProgram(programId)
        val incompatibleProgram = findProgram(incompatibleProgramId)

        adminProgramIncompatibilityValidator.validateCreate(
            program = program,
            incompatibleWithProgram = incompatibleProgram,
        )

        val incompatibility = toProgramIncompatibility(
            program = program,
            incompatibleWithProgram = incompatibleProgram,
        )

        val saved = try {
            programIncompatibilityRepository.saveAndFlush(incompatibility)
        } catch (_: DataIntegrityViolationException) {
            throw ProgramIncompatibilityErrors.alreadyExists(
                programId = programId,
                incompatibleProgramId = incompatibleProgramId,
            )
        }

        logService.record(
            user = currentUserService.userReference(),
            action = LogAction.CREATE,
            entityType = LogEntityType.PROGRAM_INCOMPATIBILITY,
            entityId = "$programId:$incompatibleProgramId",
            newValues = json(saved.toAuditSnapshot()),
        )

        return ProgramIncompatibilityResponse(
            programId = requireNotNull(program.id),
            programName = program.name,
            incompatibleWithProgramId = requireNotNull(incompatibleProgram.id),
            incompatibleWithProgramName = incompatibleProgram.name,
        )
    }

    @Transactional
    fun delete(
        programId: UUID,
        incompatibleProgramId: UUID,
    ) {
        findProgram(programId)
        findProgram(incompatibleProgramId)

        val incompatibility =
            programIncompatibilityRepository.findBetweenPrograms(
                programId = programId,
                incompatibleProgramId = incompatibleProgramId,
            )
                ?: throw ProgramIncompatibilityErrors.notFound(
                    programId = programId,
                    incompatibleProgramId = incompatibleProgramId,
                )

        val oldValues = json(incompatibility.toAuditSnapshot())
        programIncompatibilityRepository.delete(incompatibility)

        logService.record(
            user = currentUserService.userReference(),
            action = LogAction.DELETE,
            entityType = LogEntityType.PROGRAM_INCOMPATIBILITY,
            entityId = "$programId:$incompatibleProgramId",
            oldValues = oldValues,
        )
    }

    private fun findProgram(id: UUID): Program =
        programRepository
            .findById(id)
            .orElseThrow {
                ProgramErrors.notFound(id)
            }

    private fun ProgramIncompatibility.toResponseFor(
        program: Program,
    ): ProgramIncompatibilityResponse {
        val programId = requireNotNull(program.id)

        val incompatibleProgram =
            if (requireNotNull(this.program.id) == programId) {
                this.incompatibleWithProgram
            } else {
                this.program
            }

        return ProgramIncompatibilityResponse(
            programId = programId,
            programName = program.name,
            incompatibleWithProgramId = requireNotNull(incompatibleProgram.id),
            incompatibleWithProgramName = incompatibleProgram.name,
        )
    }

    private fun json(value: Any): String =
        requireNotNull(jsonMapper.writeValueAsString(value))
}
