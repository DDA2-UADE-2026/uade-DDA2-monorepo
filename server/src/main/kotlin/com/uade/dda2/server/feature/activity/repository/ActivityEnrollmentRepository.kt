package com.uade.dda2.server.feature.activity.repository

import com.uade.dda2.server.feature.activity.entity.ActivityEnrollment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import jakarta.persistence.LockModeType
import java.util.UUID

interface ActivityEnrollmentCount {
    val activityId: UUID
    val enrollmentCount: Long
}

interface ActivityEnrollmentRepository : JpaRepository<ActivityEnrollment, UUID> {
    fun existsByActivityIdAndCitizenId(activityId: UUID, citizenId: Long): Boolean

    fun countByActivityId(activityId: UUID): Long

    fun existsByCitizenIdOrAttendanceRecordedById(citizenId: Long, attendanceRecordedById: Long): Boolean

    @EntityGraph(attributePaths = ["citizen", "attendanceRecordedBy"])
    fun findAllByActivityId(activityId: UUID, pageable: Pageable): Page<ActivityEnrollment>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
        select e from ActivityEnrollment e
        join fetch e.citizen
        left join fetch e.attendanceRecordedBy
        where e.id = :id and e.activity.id = :activityId
        """,
    )
    fun findByIdAndActivityIdForUpdate(
        @Param("id") id: UUID,
        @Param("activityId") activityId: UUID,
    ): ActivityEnrollment?

    @Query(
        """
        select e.activity.id as activityId, count(e) as enrollmentCount
        from ActivityEnrollment e
        where e.activity.id in :activityIds
        group by e.activity.id
        """,
    )
    fun countByActivityIds(@Param("activityIds") activityIds: Collection<UUID>): List<ActivityEnrollmentCount>
}
