package com.uade.dda2.server.feature.benefit.repository

import com.uade.dda2.server.feature.benefit.entity.BenefitGrant
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.Lock
import jakarta.persistence.LockModeType
import org.springframework.data.repository.query.Param
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Page
import com.uade.dda2.server.feature.benefit.entity.BenefitGrantStatus
import java.time.LocalDate
import java.util.UUID

interface BenefitGrantRepository : JpaRepository<BenefitGrant, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from BenefitGrant g where g.id = :id and g.application.id = :applicationId")
    fun findByIdAndApplicationIdForUpdate(@Param("id") id: UUID, @Param("applicationId") applicationId: UUID): BenefitGrant?
    fun existsByIdAndApplicationId(id: UUID, applicationId: UUID): Boolean
    @Query("select count(g) from BenefitGrant g where g.application.id = :applicationId and g.status in (com.uade.dda2.server.feature.benefit.entity.BenefitGrantStatus.ACTIVE, com.uade.dda2.server.feature.benefit.entity.BenefitGrantStatus.SUSPENDED)")
    fun countUnfinished(@Param("applicationId") applicationId: UUID): Long
    @EntityGraph(attributePaths = ["application.user", "assignedBy"])
    fun findAllByApplicationIdOrderByAssignedAtAscIdAsc(applicationId: UUID): List<BenefitGrant>
    // Fetch only the case/program context. Catalog values, actors and history are not needed by the citizen mapper.
    @EntityGraph(attributePaths = ["application.programEdition.program"])
    fun findAllByApplicationUserId(userId: Long, pageable: Pageable): Page<BenefitGrant>
    fun existsByApplicationIdAndProgramBenefitId(applicationId: UUID, programBenefitId: UUID): Boolean
    fun existsByProgramBenefitId(programBenefitId: UUID): Boolean
    fun existsByAssignedById(userId: Long): Boolean

    // Discovery has no locks or fetched associations; a UUID cursor avoids skipping rows as previous batches change.
    @Query("""
        select distinct g.application.id from BenefitGrant g
        where g.status in :statuses and g.endDate < :today
          and g.application.status = com.uade.dda2.server.feature.application.entity.ApplicationStatus.APPROVED
          and (:afterId is null or g.application.id > :afterId)
        order by g.application.id
    """)
    fun findExpirationCandidates(@Param("statuses") statuses: Collection<BenefitGrantStatus>,
        @Param("today") today: LocalDate, @Param("afterId") afterId: UUID?, pageable: Pageable): List<UUID>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select g from BenefitGrant g where g.application.id = :applicationId
          and g.status in :statuses and g.endDate < :today order by g.id
    """)
    fun findExpiredForUpdate(@Param("applicationId") applicationId: UUID,
        @Param("statuses") statuses: Collection<BenefitGrantStatus>, @Param("today") today: LocalDate): List<BenefitGrant>

    @Query("""
        select case when count(g) > 0 then true else false end from BenefitGrant g
        where g.application.programEdition.id = :editionId and (g.startDate < :startDate or g.endDate > :endDate)
    """)
    fun existsOutsideEditionDateRange(@Param("editionId") editionId: UUID,
        @Param("startDate") startDate: LocalDate, @Param("endDate") endDate: LocalDate): Boolean
}
