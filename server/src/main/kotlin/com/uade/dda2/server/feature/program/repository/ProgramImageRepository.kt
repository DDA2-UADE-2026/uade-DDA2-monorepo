package com.uade.dda2.server.feature.program.repository

import com.uade.dda2.server.feature.program.entity.ProgramImage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface ProgramImageReferenceProjection {
    val id: UUID
    val programId: UUID
}

interface ProgramImageRepository : JpaRepository<ProgramImage, UUID> {
    fun findByProgramId(programId: UUID): ProgramImage?

    fun existsByProgramId(programId: UUID): Boolean

    @Query(
        """
            select image.id as id, program.id as programId
            from ProgramImage image
            join image.program program
            where program.id in :programIds
        """,
    )
    fun findReferencesByProgramIdIn(
        @Param("programIds") programIds: Collection<UUID>,
    ): List<ProgramImageReferenceProjection>

    @Query("select image.id from ProgramImage image where image.program.id = :programId")
    fun findImageIdByProgramId(
        @Param("programId") programId: UUID,
    ): UUID?

    @Modifying
    @Query("delete from ProgramImage image where image.program.id = :programId")
    fun deleteByProgramId(
        @Param("programId") programId: UUID,
    ): Int
}
