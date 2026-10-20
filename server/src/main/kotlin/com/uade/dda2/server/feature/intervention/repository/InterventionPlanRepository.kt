package com.uade.dda2.server.feature.intervention.repository

import com.uade.dda2.server.feature.intervention.entity.InterventionPlan
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.*
import org.springframework.data.repository.query.Param
import java.util.UUID

interface InterventionPlanRepository : JpaRepository<InterventionPlan, UUID> {
    fun existsByApplicationId(applicationId: UUID): Boolean
    fun existsByResponsibleId(userId: Long): Boolean
    @EntityGraph(attributePaths = ["responsible", "application.user"])
    fun findByApplicationId(applicationId: UUID): InterventionPlan?
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from InterventionPlan p where p.application.id = :applicationId")
    fun findByApplicationIdForUpdate(@Param("applicationId") applicationId: UUID): InterventionPlan?
}
