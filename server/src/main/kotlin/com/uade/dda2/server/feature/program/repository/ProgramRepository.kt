package com.uade.dda2.server.feature.program.repository

import com.uade.dda2.server.feature.program.entity.Program
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ProgramRepository : JpaRepository<Program, UUID> {

    fun findAllByOrderByNameAsc(pageable: Pageable): Page<Program>

    fun existsByNormalizedName(normalizedName: String): Boolean

    fun existsByNormalizedNameAndIdNot(
        normalizedName: String,
        id: UUID,
    ): Boolean

    fun existsByCreatedById(createdById: Long): Boolean
}
