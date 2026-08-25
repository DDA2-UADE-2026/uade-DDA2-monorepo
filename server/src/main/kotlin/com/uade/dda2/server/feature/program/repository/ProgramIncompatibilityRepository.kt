package com.uade.dda2.server.feature.program.repository

import com.uade.dda2.server.feature.program.entity.ProgramIncompatibility
import com.uade.dda2.server.feature.program.entity.ProgramIncompatibilityId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface ProgramIncompatibilityRepository :
    JpaRepository<ProgramIncompatibility, ProgramIncompatibilityId> {

    @Query(
        """
        SELECT pi
        FROM ProgramIncompatibility pi
        WHERE pi.program.id = :programId
           OR pi.incompatibleWithProgram.id = :programId
        """
    )
    fun findAllByProgramId(
        @Param("programId") programId: UUID,
    ): List<ProgramIncompatibility>

    @Query(
        """
        SELECT CASE WHEN COUNT(pi) > 0 THEN true ELSE false END
        FROM ProgramIncompatibility pi
        WHERE pi.program.id = :programId
           OR pi.incompatibleWithProgram.id = :programId
        """
    )
    fun existsByProgramId(
        @Param("programId") programId: UUID,
    ): Boolean

    @Query(
        """
        SELECT CASE WHEN COUNT(pi) > 0 THEN true ELSE false END
        FROM ProgramIncompatibility pi
        WHERE (
            pi.program.id = :programId
            AND pi.incompatibleWithProgram.id = :incompatibleProgramId
        )
        OR (
            pi.program.id = :incompatibleProgramId
            AND pi.incompatibleWithProgram.id = :programId
        )
        """
    )
    fun existsBetweenPrograms(
        @Param("programId") programId: UUID,
        @Param("incompatibleProgramId") incompatibleProgramId: UUID,
    ): Boolean

    @Query(
        """
        SELECT pi
        FROM ProgramIncompatibility pi
        WHERE (
            pi.program.id = :programId
            AND pi.incompatibleWithProgram.id = :incompatibleProgramId
        )
        OR (
            pi.program.id = :incompatibleProgramId
            AND pi.incompatibleWithProgram.id = :programId
        )
        """
    )
    fun findBetweenPrograms(
        @Param("programId") programId: UUID,
        @Param("incompatibleProgramId") incompatibleProgramId: UUID,
    ): ProgramIncompatibility?
}
