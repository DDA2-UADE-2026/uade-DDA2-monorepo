package com.uade.dda2.server.feature.program.mapper

import com.uade.dda2.server.feature.program.dto.response.ProgramIncompatibilityResponse
import com.uade.dda2.server.feature.program.entity.Program
import com.uade.dda2.server.feature.program.entity.ProgramIncompatibility
import com.uade.dda2.server.feature.program.entity.ProgramIncompatibilityId

fun toProgramIncompatibility(
    program: Program,
    incompatibleWithProgram: Program,
): ProgramIncompatibility {
    val programId = requireNotNull(program.id)
    val incompatibleProgramId = requireNotNull(incompatibleWithProgram.id)

    val (firstProgram, secondProgram) =
        if (programId < incompatibleProgramId) {
            program to incompatibleWithProgram
        } else {
            incompatibleWithProgram to program
        }

    val firstProgramId = requireNotNull(firstProgram.id)
    val secondProgramId = requireNotNull(secondProgram.id)

    return ProgramIncompatibility(
        id = ProgramIncompatibilityId(
            programId = firstProgramId,
            incompatibleWithProgramId = secondProgramId,
        ),
        program = firstProgram,
        incompatibleWithProgram = secondProgram,
    )
}

fun ProgramIncompatibility.toResponse(): ProgramIncompatibilityResponse =
    ProgramIncompatibilityResponse(
        programId = requireNotNull(program.id),
        programName = program.name,
        incompatibleWithProgramId = requireNotNull(incompatibleWithProgram.id),
        incompatibleWithProgramName = incompatibleWithProgram.name,
    )
