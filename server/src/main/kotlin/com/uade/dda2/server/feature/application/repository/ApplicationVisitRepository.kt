package com.uade.dda2.server.feature.application.repository

import com.uade.dda2.server.feature.application.entity.ApplicationVisit
import com.uade.dda2.server.feature.application.entity.ApplicationVisitStatus
import com.uade.dda2.server.feature.application.entity.ApplicationVisitType
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.OffsetDateTime
import java.util.UUID

interface ApplicationVisitRepository : JpaRepository<ApplicationVisit, UUID> {
    fun existsByResponsibleId(responsibleId: Long): Boolean

    fun countByApplicationId(applicationId: UUID): Long

    fun existsByApplicationIdAndStatusAndIdNot(
        applicationId: UUID,
        status: ApplicationVisitStatus,
        id: UUID,
    ): Boolean

    @EntityGraph(
        attributePaths = [
            "responsible",
            "cancelledBy",
            "application",
            "application.user",
            "application.programEdition",
            "application.programEdition.program",
        ],
    )
    @Query(
        """
        select visit from ApplicationVisit visit
        where (:applicationId is null or visit.application.id = :applicationId)
          and (:applicationNumber is null or visit.application.applicationNumber = :applicationNumber)
          and (:applicantUserId is null or visit.application.user.id = :applicantUserId)
          and (:responsibleId is null or visit.responsible.id = :responsibleId)
          and (:programId is null or visit.application.programEdition.program.id = :programId)
          and (:programEditionId is null or visit.application.programEdition.id = :programEditionId)
          and (:type is null or visit.type = :type)
          and (:status is null or visit.status = :status)
          and (:windowStart is null or visit.endsAt > :windowStart)
          and (:windowEnd is null or visit.startsAt < :windowEnd)
        """,
    )
    fun search(
        @Param("applicationId") applicationId: UUID?,
        @Param("applicationNumber") applicationNumber: Long?,
        @Param("applicantUserId") applicantUserId: Long?,
        @Param("responsibleId") responsibleId: Long?,
        @Param("programId") programId: UUID?,
        @Param("programEditionId") programEditionId: UUID?,
        @Param("type") type: ApplicationVisitType?,
        @Param("status") status: ApplicationVisitStatus?,
        @Param("windowStart") windowStart: OffsetDateTime?,
        @Param("windowEnd") windowEnd: OffsetDateTime?,
        pageable: Pageable,
    ): Page<ApplicationVisit>

    @EntityGraph(
        attributePaths = [
            "responsible",
            "cancelledBy",
            "application",
            "application.user",
            "application.programEdition",
            "application.programEdition.program",
            "application.enrollmentPeriod",
        ],
    )
    @Query("select visit from ApplicationVisit visit where visit.id = :id")
    fun findDetailById(@Param("id") id: UUID): ApplicationVisit?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(
        attributePaths = [
            "responsible",
            "cancelledBy",
            "application",
            "application.user",
            "application.assignedWorker",
        ],
    )
    @Query("select visit from ApplicationVisit visit where visit.id = :id")
    fun findByIdForUpdate(@Param("id") id: UUID): ApplicationVisit?

    @EntityGraph(attributePaths = ["responsible", "application", "application.user"])
    @Query(
        """
        select distinct visit from ApplicationVisit visit
        where visit.status = :status
          and (visit.responsible.id in :userIds or visit.application.user.id in :userIds)
          and visit.startsAt < :rangeEnd
          and visit.endsAt > :rangeStart
        """,
    )
    fun findByParticipantIdsInRange(
        @Param("userIds") userIds: Collection<Long>,
        @Param("rangeStart") rangeStart: OffsetDateTime,
        @Param("rangeEnd") rangeEnd: OffsetDateTime,
        @Param("status") status: ApplicationVisitStatus = ApplicationVisitStatus.SCHEDULED,
    ): List<ApplicationVisit>
}
