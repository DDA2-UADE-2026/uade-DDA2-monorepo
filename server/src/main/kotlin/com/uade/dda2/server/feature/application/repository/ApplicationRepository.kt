package com.uade.dda2.server.feature.application.repository

import com.uade.dda2.server.feature.application.entity.Application
import com.uade.dda2.server.feature.application.entity.ApplicationStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import jakarta.persistence.LockModeType
import java.util.UUID
import java.time.LocalDate

interface ApplicationRepository : JpaRepository<Application, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Application a where a.id = :id and a.user.id = :userId")
    fun findByIdAndUserIdForUpdate(@Param("id") id: UUID, @Param("userId") userId: Long): Application?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Application a where a.id = :id")
    fun findByIdForUpdate(@Param("id") id: UUID): Application?

    @EntityGraph(attributePaths = ["programEdition.program"])
    fun findByIdAndUserId(id: UUID, userId: Long): Application?
    @EntityGraph(attributePaths = ["programEdition.program"])
    fun findAllByUserId(userId: Long, pageable: Pageable): Page<Application>

    // Administrative read side: the titular and the actor are part of the response, so they are fetched with the page.
    @EntityGraph(attributePaths = ["user", "registeredBy", "assignedWorker", "programEdition.program"])
    override fun findAll(pageable: Pageable): Page<Application>

    @EntityGraph(attributePaths = ["user", "registeredBy", "assignedWorker", "programEdition.program"])
    fun findAllByProgramEditionProgramId(programId: UUID, pageable: Pageable): Page<Application>

    fun countByProgramEditionIdAndStatus(programEditionId: UUID, status: ApplicationStatus): Long
    fun existsByResolvedById(userId: Long): Boolean

    @EntityGraph(attributePaths = ["user", "registeredBy", "assignedWorker", "resolvedBy", "programEdition.program"])
    fun findDetailById(id: UUID): Application?

    @Query("""
        select case when count(a) > 0 then true else false end from Application a
        where a.user.id = :userId and a.id <> :applicationId and a.status = :status
          and a.programEdition.program.id in :programIds
          and a.programEdition.startDate <= :endDate and a.programEdition.endDate >= :startDate
    """)
    fun existsOverlappingApproval(
        @Param("userId") userId: Long, @Param("applicationId") applicationId: UUID,
        @Param("programIds") programIds: Collection<UUID>, @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate, @Param("status") status: ApplicationStatus = ApplicationStatus.APPROVED,
    ): Boolean

    fun findByUserIdAndIdempotencyKey(userId: Long, idempotencyKey: String): Application?
    fun existsByUserIdAndEnrollmentPeriodId(userId: Long, enrollmentPeriodId: UUID): Boolean
    fun existsByUserIdAndProgramEditionIdAndStatusNotIn(userId: Long, programEditionId: UUID, statuses: Collection<ApplicationStatus>): Boolean
    fun existsByProgramEditionId(programEditionId: UUID): Boolean
    fun existsByUserIdOrAssignedWorkerIdOrRegisteredById(userId: Long, assignedWorkerId: Long, registeredById: Long): Boolean
}
