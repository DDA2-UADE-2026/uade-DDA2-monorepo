package com.uade.dda2.server.feature.program.entity

import com.uade.dda2.server.feature.program.entity.enums.ProgramRequirementType

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "program_requirement")
class ProgramRequirement(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "program_edition_id",
        nullable = false,
    )
    var programEdition: ProgramEdition,

    @Enumerated(EnumType.STRING)
    @Column(
        nullable = false,
        length = 50,
    )
    var type: ProgramRequirementType,

    @Column(
        nullable = false,
        length = 255,
    )
    var value: String,

    @Column(
        length = 500,
    )
    var description: String? = null,
)