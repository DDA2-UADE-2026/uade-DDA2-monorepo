package com.uade.dda2.server.feature.program.entity

import com.uade.dda2.server.feature.program.entity.enums.ProgramBenefitType

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
import java.math.BigDecimal
import java.util.UUID

@Entity
@Table(name = "program_benefit")
class ProgramBenefit(

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
        name = "benefit_type",
        nullable = false,
        length = 50,
    )
    var benefitType: ProgramBenefitType,

    @Column(
        length = 500,
    )
    var description: String? = null,

    @Column(
        precision = 19,
        scale = 2,
    )
    var amount: BigDecimal? = null,
)