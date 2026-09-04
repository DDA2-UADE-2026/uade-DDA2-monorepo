package com.uade.dda2.server.feature.application.repository

import com.uade.dda2.server.feature.application.entity.Application
import com.uade.dda2.server.feature.application.entity.ApplicationStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ApplicationRepository : JpaRepository<Application, UUID> {
    fun findByIdAndUserId(id: UUID, userId: Long): Application?
    fun findAllByUserId(userId: Long, pageable: Pageable): Page<Application>
    fun findByUserIdAndIdempotencyKey(userId: Long, idempotencyKey: String): Application?
    fun existsByUserIdAndEnrollmentPeriodId(userId: Long, enrollmentPeriodId: UUID): Boolean
    fun existsByUserIdAndProgramEditionIdAndStatusNotIn(userId: Long, programEditionId: UUID, statuses: Collection<ApplicationStatus>): Boolean
    fun existsByUserIdOrAssignedWorkerIdOrRegisteredById(userId: Long, assignedWorkerId: Long, registeredById: Long): Boolean
}
