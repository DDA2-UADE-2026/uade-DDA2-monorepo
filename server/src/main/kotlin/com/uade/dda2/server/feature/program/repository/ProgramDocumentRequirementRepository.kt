package com.uade.dda2.server.feature.program.repository

import com.uade.dda2.server.feature.program.entity.ProgramDocumentRequirement
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ProgramDocumentRequirementRepository : JpaRepository<ProgramDocumentRequirement, UUID> {
    fun findAllByProgramEditionIdOrderByNameAsc(programEditionId: UUID): List<ProgramDocumentRequirement>
    fun findAllByProgramEditionIdIn(programEditionIds: Collection<UUID>): List<ProgramDocumentRequirement>
    fun findByIdAndProgramEditionId(id: UUID, programEditionId: UUID): ProgramDocumentRequirement?
    fun existsByProgramEditionId(programEditionId: UUID): Boolean
    fun existsByProgramEditionIdAndCode(programEditionId: UUID, code: String): Boolean
    fun existsByProgramEditionIdAndCodeAndIdNot(programEditionId: UUID, code: String, id: UUID): Boolean
}
