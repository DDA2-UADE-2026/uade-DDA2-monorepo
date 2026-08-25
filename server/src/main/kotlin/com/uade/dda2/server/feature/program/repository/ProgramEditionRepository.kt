package com.uade.dda2.server.feature.program.repository

import com.uade.dda2.server.feature.program.entity.ProgramEdition
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ProgramEditionRepository : JpaRepository<ProgramEdition, UUID> {

    fun findAllByProgramIdOrderByStartDateDesc(
        programId: UUID,
        pageable: Pageable,
    ): Page<ProgramEdition>

    fun existsByProgramIdAndNormalizedName(
        programId: UUID,
        normalizedName: String,
    ): Boolean

    fun existsByProgramIdAndNormalizedNameAndIdNot(
        programId: UUID,
        normalizedName: String,
        id: UUID,
    ): Boolean

    fun existsByProgramId(programId: UUID): Boolean

    fun existsByCreatedById(createdById: Long): Boolean
}
