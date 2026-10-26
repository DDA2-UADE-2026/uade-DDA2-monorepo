package com.uade.dda2.server.feature.intervention.repository

import com.uade.dda2.server.feature.intervention.entity.InterventionPlan
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.*
import org.springframework.data.repository.query.Param
import java.util.UUID
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface InterventionPlanRepository : JpaRepository<InterventionPlan, UUID> {
    @Query(value = PUBLIC_PLAN_SELECT + " where p.application.user.id = :userId",
        countQuery = "select count(p) from InterventionPlan p where p.application.user.id = :userId")
    fun findPublicPlansByUserId(@Param("userId") userId: Long, pageable: Pageable): Page<CitizenInterventionPlanRow>

    @Query(PUBLIC_PLAN_SELECT + " where p.id = :id and p.application.user.id = :userId")
    fun findPublicPlanByIdAndUserId(@Param("id") id: UUID, @Param("userId") userId: Long): CitizenInterventionPlanRow?
    fun existsByApplicationId(applicationId: UUID): Boolean
    fun existsByResponsibleId(userId: Long): Boolean
    @EntityGraph(attributePaths = ["responsible", "application.user"])
    fun findByApplicationId(applicationId: UUID): InterventionPlan?
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from InterventionPlan p where p.application.id = :applicationId")
    fun findByApplicationIdForUpdate(@Param("applicationId") applicationId: UUID): InterventionPlan?
}

private const val PUBLIC_PLAN_SELECT = """select p.id as id, p.application.id as applicationId,
    p.application.applicationNumber as applicationNumber, p.application.programEdition.program.id as programId,
    p.application.programEdition.program.name as programName, p.application.programEdition.id as programEditionId,
    p.application.programEdition.name as programEditionName, p.createdAt as createdAt from InterventionPlan p"""
