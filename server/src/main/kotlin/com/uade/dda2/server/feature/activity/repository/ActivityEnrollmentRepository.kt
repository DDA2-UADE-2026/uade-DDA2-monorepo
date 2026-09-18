package com.uade.dda2.server.feature.activity.repository

import com.uade.dda2.server.feature.activity.entity.ActivityEnrollment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface ActivityEnrollmentCount {
    val activityId: UUID
    val enrollmentCount: Long
}

interface ActivityEnrollmentRepository : JpaRepository<ActivityEnrollment, UUID> {
    fun existsByActivityIdAndCitizenId(activityId: UUID, citizenId: Long): Boolean

    fun countByActivityId(activityId: UUID): Long

    fun existsByCitizenId(citizenId: Long): Boolean

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
