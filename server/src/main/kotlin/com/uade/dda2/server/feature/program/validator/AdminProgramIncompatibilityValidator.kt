package com.uade.dda2.server.feature.program.validator

import com.uade.dda2.server.feature.program.entity.Program
import com.uade.dda2.server.feature.program.error.ProgramIncompatibilityErrors
import com.uade.dda2.server.feature.program.repository.ProgramIncompatibilityRepository
import org.springframework.stereotype.Component

@Component
class AdminProgramIncompatibilityValidator(
    private val programIncompatibilityRepository: ProgramIncompatibilityRepository,
) {

    fun validateCreate(
        program: Program,
        incompatibleWithProgram: Program,
    ) {
        val programId = requireNotNull(program.id)
        val incompatibleProgramId = requireNotNull(incompatibleWithProgram.id)

        if (programId == incompatibleProgramId) {
            throw ProgramIncompatibilityErrors.sameProgram()
        }

        if (
            programIncompatibilityRepository.existsBetweenPrograms(
                programId = programId,
                incompatibleProgramId = incompatibleProgramId,
            )
        ) {
            throw ProgramIncompatibilityErrors.alreadyExists(
                programId = programId,
                incompatibleProgramId = incompatibleProgramId,
            )
        }
    }
}
