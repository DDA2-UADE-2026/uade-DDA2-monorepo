package com.uade.dda2.server.feature.intervention.repository

import com.uade.dda2.server.feature.intervention.entity.InterventionFollowUpPublication
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.*
import org.springframework.data.repository.query.Param
import java.util.UUID

interface InterventionPublicationRepository : JpaRepository<InterventionFollowUpPublication, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from InterventionFollowUpPublication p where p.followUp.id = :followUpId")
    fun findByFollowUpIdForUpdate(@Param("followUpId") followUpId: UUID): InterventionFollowUpPublication?
    @EntityGraph(attributePaths = ["updatedBy"])
    fun findAllByFollowUpIdIn(followUpIds: Collection<UUID>): List<InterventionFollowUpPublication>
    fun existsByUpdatedById(userId: Long): Boolean

    @Query(value = """select p.followUp.id as id, p.followUp.action.id as actionId, p.followUp.occurredOn as occurredOn,
        p.followUp.status as status, p.summary as summary, p.publishedAt as publishedAt
        from InterventionFollowUpPublication p where p.visible = true and p.followUp.action.id = :actionId
          and p.followUp.action.objective.plan.id = :planId and p.followUp.action.objective.plan.application.user.id = :userId
        order by p.followUp.sequenceNumber asc""",
        countQuery = """select count(p) from InterventionFollowUpPublication p where p.visible = true
            and p.followUp.action.id = :actionId and p.followUp.action.objective.plan.id = :planId
            and p.followUp.action.objective.plan.application.user.id = :userId""")
    fun findVisibleByOwnerAndAction(@Param("userId") userId: Long, @Param("planId") planId: UUID,
        @Param("actionId") actionId: UUID, pageable: Pageable): Page<CitizenInterventionFollowUpRow>
}
