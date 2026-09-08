package com.uade.dda2.server.feature.program.entity

import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapsId
import jakarta.persistence.Table

@Entity
@Table(name = "program_incompatibility")
class ProgramIncompatibility(

    @EmbeddedId
    var id: ProgramIncompatibilityId = ProgramIncompatibilityId(),

    @MapsId("programId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "program_id",
        nullable = false,
    )
    var program: Program,

    @MapsId("incompatibleWithProgramId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "incompatible_with_program_id",
        nullable = false,
    )
    var incompatibleWithProgram: Program,
)