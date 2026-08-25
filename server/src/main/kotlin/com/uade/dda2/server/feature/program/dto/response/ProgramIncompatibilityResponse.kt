package com.uade.dda2.server.feature.program.dto.response

import java.util.UUID

data class ProgramIncompatibilityResponse(
    val programId: UUID,
    val programName: String,
    val incompatibleWithProgramId: UUID,
    val incompatibleWithProgramName: String,
)