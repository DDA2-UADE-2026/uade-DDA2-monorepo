package com.uade.dda2.server.feature.program.repository

import com.uade.dda2.server.feature.program.entity.ProgramEdition
import com.uade.dda2.server.feature.program.entity.enums.ProgramEditionStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate
import java.util.UUID
import jakarta.persistence.LockModeType

interface ProgramEditionRepository : JpaRepository<ProgramEdition, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
            SELECT e
            FROM ProgramEdition e
            JOIN FETCH e.program
            WHERE e.id = :id
        """,
    )
    fun findByIdForUpdate(
        @Param("id") id: UUID,
    ): ProgramEdition?

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
