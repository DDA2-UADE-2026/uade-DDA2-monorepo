package com.uade.dda2.server.feature.application.repository

import com.uade.dda2.server.feature.application.entity.ApplicationVisitRescheduleRequest
import com.uade.dda2.server.feature.application.entity.ApplicationVisitRescheduleRequestStatus
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

interface ApplicationVisitRescheduleRequestRepository :
    JpaRepository<ApplicationVisitRescheduleRequest, UUID> {

    fun existsByVisitIdAndStatus(visitId: UUID, status: ApplicationVisitRescheduleRequestStatus): Boolean

    fun existsByRequestedByIdOrReviewedById(requestedById: Long, reviewedById: Long): Boolean

    @EntityGraph(attributePaths = ["visit", "visit.application"])
    @Query(
        "select request from ApplicationVisitRescheduleRequest request " +
            "where request.visit.id = :visitId and request.visit.application.id = :applicationId " +
            "and request.visit.application.user.id = :userId",
    )
    fun findAllOwn(
        @Param("applicationId") applicationId: UUID,
        @Param("visitId") visitId: UUID,
        @Param("userId") userId: Long,
        pageable: Pageable,
    ): Page<ApplicationVisitRescheduleRequest>

    @EntityGraph(attributePaths = ["visit", "visit.application"])
    @Query(
        "select request from ApplicationVisitRescheduleRequest request " +
            "where request.id = :requestId and request.visit.id = :visitId " +
            "and request.visit.application.id = :applicationId and request.visit.application.user.id = :userId",
    )
    fun findOwnDetail(
        @Param("requestId") requestId: UUID,
        @Param("applicationId") applicationId: UUID,
        @Param("visitId") visitId: UUID,
        @Param("userId") userId: Long,
    ): ApplicationVisitRescheduleRequest?

    @EntityGraph(
        attributePaths = [
            "requestedBy",
            "reviewedBy",
            "visit",
            "visit.responsible",
            "visit.application",
            "visit.application.user",
            "visit.application.programEdition",
            "visit.application.programEdition.program",
        ],
    )
    @Query(
        """
        select request from ApplicationVisitRescheduleRequest request
        where (:status is null or request.status = :status)
          and (:applicationId is null or request.visit.application.id = :applicationId)
          and (:applicationNumber is null or request.visit.application.applicationNumber = :applicationNumber)
          and (:visitId is null or request.visit.id = :visitId)
          and (:applicantUserId is null or request.visit.application.user.id = :applicantUserId)
          and (:responsibleId is null or request.visit.responsible.id = :responsibleId)
          and (:programId is null or request.visit.application.programEdition.program.id = :programId)
          and (:programEditionId is null or request.visit.application.programEdition.id = :programEditionId)
          and (:requestedFrom is null or request.requestedAt >= :requestedFrom)
          and (:requestedTo is null or request.requestedAt < :requestedTo)
        """,
    )
    fun search(
        @Param("status") status: ApplicationVisitRescheduleRequestStatus?,
        @Param("applicationId") applicationId: UUID?,
        @Param("applicationNumber") applicationNumber: Long?,
        @Param("visitId") visitId: UUID?,
        @Param("applicantUserId") applicantUserId: Long?,
        @Param("responsibleId") responsibleId: Long?,
        @Param("programId") programId: UUID?,
        @Param("programEditionId") programEditionId: UUID?,
        @Param("requestedFrom") requestedFrom: OffsetDateTime?,
        @Param("requestedTo") requestedTo: OffsetDateTime?,
        pageable: Pageable,
    ): Page<ApplicationVisitRescheduleRequest>

    @EntityGraph(
        attributePaths = [
            "requestedBy",
            "reviewedBy",
            "visit",
            "visit.responsible",
            "visit.application",
            "visit.application.user",
            "visit.application.programEdition",
            "visit.application.programEdition.program",
        ],
    )
    @Query("select request from ApplicationVisitRescheduleRequest request where request.id = :id")
    fun findAdminDetailById(@Param("id") id: UUID): ApplicationVisitRescheduleRequest?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = ["requestedBy", "reviewedBy", "visit"])
    @Query("select request from ApplicationVisitRescheduleRequest request where request.id = :id")
    fun findByIdForUpdate(@Param("id") id: UUID): ApplicationVisitRescheduleRequest?

    @EntityGraph(attributePaths = ["visit"])
    @Query(
        "select request from ApplicationVisitRescheduleRequest request " +
            "where request.visit.id in :visitIds order by request.requestedAt desc, request.id desc",
    )
    fun findLatestCandidates(@Param("visitIds") visitIds: Collection<UUID>): List<ApplicationVisitRescheduleRequest>
}
