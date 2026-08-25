package com.uade.dda2.server.feature.program.validator

import com.uade.dda2.server.feature.program.dto.request.CreateProgramRequest
import com.uade.dda2.server.feature.program.dto.request.UpdateProgramRequest
import com.uade.dda2.server.feature.program.entity.Program
import com.uade.dda2.server.feature.program.error.ProgramErrors
import com.uade.dda2.server.feature.program.repository.ProgramEditionRepository
import com.uade.dda2.server.feature.program.repository.ProgramIncompatibilityRepository
import com.uade.dda2.server.feature.program.repository.ProgramRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ProgramValidator(
    private val programRepository: ProgramRepository,
    private val programEditionRepository: ProgramEditionRepository,
    private val programIncompatibilityRepository: ProgramIncompatibilityRepository,
) {

    fun validateCreate(request: CreateProgramRequest) {
        validateUniqueName(request.name)
    }

    fun validateUpdate(
        program: Program,
        request: UpdateProgramRequest,
    ) {
        validateUniqueName(
            name = request.name,
            excludeId = requireNotNull(program.id),
        )
    }

    fun validateDelete(program: Program) {
        val programId = requireNotNull(program.id)

        if (programEditionRepository.existsByProgramId(programId)) {
            throw ProgramErrors.hasEditions(programId)
        }

        if (programIncompatibilityRepository.existsByProgramId(programId)) {
            throw ProgramErrors.hasIncompatibilities(programId)
        }
    }

    private fun validateUniqueName(
        name: String,
        excludeId: UUID? = null,
    ) {
        val trimmedName = name.trim()
        val normalizedName = trimmedName.lowercase()

        val exists = if (excludeId == null) {
            programRepository.existsByNormalizedName(normalizedName)
        } else {
            programRepository.existsByNormalizedNameAndIdNot(
                normalizedName = normalizedName,
                id = excludeId,
            )
        }

        if (exists) {
            throw ProgramErrors.nameAlreadyExists(trimmedName)
        }
    }
}
