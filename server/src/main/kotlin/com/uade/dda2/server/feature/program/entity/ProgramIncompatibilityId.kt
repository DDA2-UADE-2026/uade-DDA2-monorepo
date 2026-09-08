package com.uade.dda2.server.feature.program.entity

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.io.Serializable
import java.util.UUID

@Embeddable
data class ProgramIncompatibilityId(

    @Column(name = "program_id")
    var programId: UUID? = null,

    @Column(name = "incompatible_with_program_id")
    var incompatibleWithProgramId: UUID? = null,

    ) : Serializable