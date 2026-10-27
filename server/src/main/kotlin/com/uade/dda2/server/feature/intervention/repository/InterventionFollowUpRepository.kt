package com.uade.dda2.server.feature.intervention.repository

import com.uade.dda2.server.feature.intervention.entity.InterventionFollowUp
import org.springframework.data.jpa.repository.*
import java.util.UUID
import jakarta.persistence.LockModeType
import org.springframework.data.repository.query.Param

interface InterventionFollowUpRepository : JpaRepository<InterventionFollowUp, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from InterventionFollowUp f where f.id = :id and f.action.id = :actionId")
    fun findByIdAndActionIdForUpdate(@Param("id") id: UUID, @Param("actionId") actionId: UUID): InterventionFollowUp?
    fun existsByAuthorId(userId: Long): Boolean
    fun findFirstByActionIdOrderBySequenceNumberDesc(actionId: UUID): InterventionFollowUp?
    @EntityGraph(attributePaths = ["author"])
    fun findAllByActionIdOrderBySequenceNumberAsc(actionId: UUID): List<InterventionFollowUp>
}
