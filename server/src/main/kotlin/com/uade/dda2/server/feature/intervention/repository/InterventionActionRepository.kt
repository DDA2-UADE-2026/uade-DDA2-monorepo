package com.uade.dda2.server.feature.intervention.repository

import com.uade.dda2.server.feature.intervention.entity.InterventionAction
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.*
import org.springframework.data.repository.query.Param
import java.util.UUID

interface InterventionActionRepository : JpaRepository<InterventionAction, UUID> {
    fun findByIdAndObjectivePlanId(id: UUID, planId: UUID): InterventionAction?
    // EXISTS validates ownership without adding objective/plan rows to the outer locking query.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""select a from InterventionAction a where a.id = :id and exists
        (select o.id from InterventionObjective o where o.id = a.objective.id and o.plan.id = :planId)""")
    fun findByIdAndPlanIdForUpdate(@Param("id") id: UUID, @Param("planId") planId: UUID): InterventionAction?
}
