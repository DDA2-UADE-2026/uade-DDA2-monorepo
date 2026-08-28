package com.uade.dda2.server.feature.program.repository

import com.uade.dda2.server.feature.program.entity.ProgramEdition
import com.uade.dda2.server.feature.program.entity.enums.ProgramEditionStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate
import java.util.UUID

interface ProgramEditionRepository : JpaRepository<ProgramEdition, UUID> {

    fun findAllByProgramIdOrderByStartDateDesc(
        programId: UUID,
        pageable: Pageable,
    ): Page<ProgramEdition>

    fun findAllByProgramIdAndStatusNotOrderByNameAsc(
        programId: UUID,
        status: ProgramEditionStatus,
    ): List<ProgramEdition>

    fun findAllByProgramIdInAndStatusAndEndDateGreaterThanEqualOrderByStartDateAsc(
        programIds: Collection<UUID>,
        status: ProgramEditionStatus,
        fromDate: LocalDate,
    ): List<ProgramEdition>

    fun findAllByProgramIdAndStatusAndEndDateGreaterThanEqualOrderByStartDateAsc(
        programId: UUID,
        status: ProgramEditionStatus,
        fromDate: LocalDate,
    ): List<ProgramEdition>

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
