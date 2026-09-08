package com.uade.dda2.server.feature.program.repository

import com.uade.dda2.server.feature.program.entity.ProgramBenefit
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ProgramBenefitRepository :
    JpaRepository<ProgramBenefit, UUID> {

    fun findAllByProgramEditionId(
        programEditionId: UUID,
    ): List<ProgramBenefit>

    fun findAllByProgramEditionIdIn(
        programEditionIds: Collection<UUID>,
    ): List<ProgramBenefit>

    fun findByIdAndProgramEditionId(
        id: UUID,
        programEditionId: UUID,
    ): ProgramBenefit?

    fun existsByProgramEditionId(
        programEditionId: UUID,
    ): Boolean
}
