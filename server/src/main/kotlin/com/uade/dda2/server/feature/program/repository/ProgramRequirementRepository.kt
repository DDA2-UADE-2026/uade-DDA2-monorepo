package com.uade.dda2.server.feature.program.repository

import com.uade.dda2.server.feature.program.entity.ProgramRequirement
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ProgramRequirementRepository :
    JpaRepository<ProgramRequirement, UUID> {

    fun findAllByProgramEditionId(
        programEditionId: UUID,
    ): List<ProgramRequirement>

    fun findAllByProgramEditionIdIn(
        programEditionIds: Collection<UUID>,
    ): List<ProgramRequirement>

    fun findByIdAndProgramEditionId(
        id: UUID,
        programEditionId: UUID,
    ): ProgramRequirement?

    fun existsByProgramEditionId(
        programEditionId: UUID,
    ): Boolean
}
