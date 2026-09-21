package com.uade.dda2.server.feature.activity.repository

import com.uade.dda2.server.feature.activity.entity.Activity
import com.uade.dda2.server.feature.activity.entity.ActivityStatus
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface ActivityRepository : JpaRepository<Activity, UUID> {
    fun findAllByOrderByCreatedAtDesc(pageable: Pageable): Page<Activity>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Activity a join fetch a.createdBy where a.id = :id")
    fun findByIdForUpdate(@Param("id") id: UUID): Activity?

    fun existsByCreatedById(createdById: Long): Boolean

    fun findAllByStatusOrderByStartDateAsc(status: ActivityStatus, pageable: Pageable): Page<Activity>

    fun findByIdAndStatus(id: UUID, status: ActivityStatus): Activity?

    fun findAllByStatusInOrderByStartDateDesc(statuses: Collection<ActivityStatus>, pageable: Pageable): Page<Activity>
}
