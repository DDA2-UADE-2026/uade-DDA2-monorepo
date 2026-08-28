package com.uade.dda2.server.feature.program.repository

import com.uade.dda2.server.feature.program.entity.Program
import com.uade.dda2.server.feature.program.entity.enums.ProgramEditionStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate
import java.util.UUID

interface ProgramRepository : JpaRepository<Program, UUID> {

    fun findAllByOrderByNameAsc(pageable: Pageable): Page<Program>

    fun findAllByOrderByNameAsc(): List<Program>

    @Query(
        value = """
            SELECT p
            FROM Program p
            WHERE EXISTS (
                SELECT e.id
                FROM ProgramEdition e
                WHERE e.program = p
                  AND e.status = :status
                  AND e.endDate >= :fromDate
            )
            ORDER BY p.name ASC
        """,
        countQuery = """
            SELECT COUNT(p)
            FROM Program p
            WHERE EXISTS (
                SELECT e.id
                FROM ProgramEdition e
                WHERE e.program = p
                  AND e.status = :status
                  AND e.endDate >= :fromDate
            )
        """,
    )
    fun findAvailable(
        @Param("status") status: ProgramEditionStatus,
        @Param("fromDate") fromDate: LocalDate,
        pageable: Pageable,
    ): Page<Program>

    @Query(
        """
            SELECT p
            FROM Program p
            WHERE p.id = :id
              AND EXISTS (
                  SELECT e.id
                  FROM ProgramEdition e
                  WHERE e.program = p
                    AND e.status = :status
                    AND e.endDate >= :fromDate
              )
        """,
    )
    fun findAvailableById(
        @Param("id") id: UUID,
        @Param("status") status: ProgramEditionStatus,
        @Param("fromDate") fromDate: LocalDate,
    ): Program?

    fun existsByNormalizedName(normalizedName: String): Boolean

    fun existsByNormalizedNameAndIdNot(
        normalizedName: String,
        id: UUID,
    ): Boolean

    fun existsByCreatedById(createdById: Long): Boolean
}
