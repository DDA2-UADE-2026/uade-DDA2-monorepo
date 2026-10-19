package com.uade.dda2.server.feature.benefit.repository

import com.uade.dda2.server.feature.benefit.entity.BenefitGrantChange
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface BenefitGrantChangeRepository : JpaRepository<BenefitGrantChange, UUID> {
    @EntityGraph(attributePaths = ["responsible"])
    fun findAllByGrantIdOrderBySequenceNumberAsc(grantId: UUID): List<BenefitGrantChange>
    @Query("select coalesce(max(c.sequenceNumber), 0) from BenefitGrantChange c where c.grant.id = :grantId")
    fun lastSequence(@Param("grantId") grantId: UUID): Int
    fun existsByResponsibleId(userId: Long): Boolean
}
