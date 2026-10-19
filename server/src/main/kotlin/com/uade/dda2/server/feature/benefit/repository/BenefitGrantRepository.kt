package com.uade.dda2.server.feature.benefit.repository

import com.uade.dda2.server.feature.benefit.entity.BenefitGrant
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate
import java.util.UUID

interface BenefitGrantRepository : JpaRepository<BenefitGrant, UUID> {
    @EntityGraph(attributePaths = ["application.user", "assignedBy"])
    fun findAllByApplicationIdOrderByAssignedAtAscIdAsc(applicationId: UUID): List<BenefitGrant>
    fun existsByApplicationIdAndProgramBenefitId(applicationId: UUID, programBenefitId: UUID): Boolean
    fun existsByProgramBenefitId(programBenefitId: UUID): Boolean
    fun existsByAssignedById(userId: Long): Boolean

    @Query("""
        select case when count(g) > 0 then true else false end from BenefitGrant g
        where g.application.programEdition.id = :editionId and (g.startDate < :startDate or g.endDate > :endDate)
    """)
    fun existsOutsideEditionDateRange(@Param("editionId") editionId: UUID,
        @Param("startDate") startDate: LocalDate, @Param("endDate") endDate: LocalDate): Boolean
}
