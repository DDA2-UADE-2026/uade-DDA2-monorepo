package com.uade.dda2.server.feature.intervention.repository

import com.uade.dda2.server.feature.intervention.entity.InterventionFollowUp
import org.springframework.data.jpa.repository.*
import java.util.UUID

interface InterventionFollowUpRepository : JpaRepository<InterventionFollowUp, UUID> {
    fun existsByAuthorId(userId: Long): Boolean
    fun findFirstByActionIdOrderBySequenceNumberDesc(actionId: UUID): InterventionFollowUp?
    @EntityGraph(attributePaths = ["author"])
    fun findAllByActionIdOrderBySequenceNumberAsc(actionId: UUID): List<InterventionFollowUp>
}
