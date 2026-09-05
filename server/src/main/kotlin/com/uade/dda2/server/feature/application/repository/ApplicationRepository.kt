package com.uade.dda2.server.feature.application.repository

import com.uade.dda2.server.feature.application.entity.Application
import com.uade.dda2.server.feature.application.entity.ApplicationStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import jakarta.persistence.LockModeType
import java.util.UUID

interface ApplicationRepository : JpaRepository<Application, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Application a join fetch a.programEdition where a.id = :id and a.user.id = :userId")
    fun findByIdAndUserIdForUpdate(@Param("id") id: UUID, @Param("userId") userId: Long): Application?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Application a join fetch a.programEdition where a.id = :id")
    fun findByIdForUpdate(@Param("id") id: UUID): Application?

    fun findByIdAndUserId(id: UUID, userId: Long): Application?
    fun findAllByUserId(userId: Long, pageable: Pageable): Page<Application>
    fun findByUserIdAndIdempotencyKey(userId: Long, idempotencyKey: String): Application?
    fun existsByUserIdAndEnrollmentPeriodId(userId: Long, enrollmentPeriodId: UUID): Boolean
    fun existsByUserIdAndProgramEditionIdAndStatusNotIn(userId: Long, programEditionId: UUID, statuses: Collection<ApplicationStatus>): Boolean
    fun existsByProgramEditionId(programEditionId: UUID): Boolean
    fun existsByUserIdOrAssignedWorkerIdOrRegisteredById(userId: Long, assignedWorkerId: Long, registeredById: Long): Boolean
}
