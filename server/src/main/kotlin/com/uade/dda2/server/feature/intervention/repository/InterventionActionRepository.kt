package com.uade.dda2.server.feature.intervention.repository

import com.uade.dda2.server.feature.intervention.entity.InterventionAction
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.*
import org.springframework.data.repository.query.Param
import java.util.UUID

interface InterventionActionRepository : JpaRepository<InterventionAction, UUID> {
    fun existsByIdAndObjectivePlanId(id: UUID, planId: UUID): Boolean
    @Query("""select a.objective.plan.id as planId, count(a) as actionCount,
        sum(case when a.status = com.uade.dda2.server.feature.intervention.entity.InterventionActionStatus.PENDING then 1 else 0 end) as pendingCount,
        sum(case when a.status = com.uade.dda2.server.feature.intervention.entity.InterventionActionStatus.COMPLETED then 1 else 0 end) as completedCount
        from InterventionAction a where a.objective.plan.id in :planIds group by a.objective.plan.id""")
    fun countStatesByPlanIds(@Param("planIds") planIds: Collection<UUID>): List<InterventionPlanActionCounts>

    @Query("""select a.objective.id as objectiveId, a.objective.description as objectiveDescription, a.id as id,
        a.description as description, a.startDate as startDate, a.dueDate as dueDate, a.status as status
        from InterventionAction a where a.objective.plan.id = :planId and a.objective.plan.application.user.id = :userId
        order by a.objective.position, a.position""")
    fun findPublicStructure(@Param("planId") planId: UUID, @Param("userId") userId: Long): List<CitizenInterventionActionRow>
    fun findByIdAndObjectivePlanId(id: UUID, planId: UUID): InterventionAction?
    // EXISTS validates ownership without adding objective/plan rows to the outer locking query.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""select a from InterventionAction a where a.id = :id and exists
        (select o.id from InterventionObjective o where o.id = a.objective.id and o.plan.id = :planId)""")
    fun findByIdAndPlanIdForUpdate(@Param("id") id: UUID, @Param("planId") planId: UUID): InterventionAction?
}
