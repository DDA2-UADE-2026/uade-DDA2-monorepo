package com.uade.dda2.server.feature.program.service

import com.uade.dda2.server.feature.program.dto.response.ProgramIncompatibilityResponse
import com.uade.dda2.server.feature.program.entity.Program
import com.uade.dda2.server.feature.program.entity.ProgramIncompatibility
import com.uade.dda2.server.feature.program.error.ProgramErrors
import com.uade.dda2.server.feature.program.error.ProgramIncompatibilityErrors
import com.uade.dda2.server.feature.program.mapper.toProgramIncompatibility
import com.uade.dda2.server.feature.program.repository.ProgramIncompatibilityRepository
import com.uade.dda2.server.feature.program.repository.ProgramRepository
import com.uade.dda2.server.feature.program.validator.ProgramIncompatibilityValidator
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ProgramIncompatibilityService(
    private val programRepository: ProgramRepository,
    private val programIncompatibilityRepository: ProgramIncompatibilityRepository,
    private val programIncompatibilityValidator: ProgramIncompatibilityValidator,
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

        programIncompatibilityValidator.validateCreate(
            program = program,
            incompatibleWithProgram = incompatibleProgram,
        )

        val incompatibility = toProgramIncompatibility(
            program = program,
            incompatibleWithProgram = incompatibleProgram,
        )

        try {
            programIncompatibilityRepository.saveAndFlush(incompatibility)
        } catch (_: DataIntegrityViolationException) {
            throw ProgramIncompatibilityErrors.alreadyExists(
                programId = programId,
                incompatibleProgramId = incompatibleProgramId,
            )
        }

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

        programIncompatibilityRepository.delete(incompatibility)
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
}
