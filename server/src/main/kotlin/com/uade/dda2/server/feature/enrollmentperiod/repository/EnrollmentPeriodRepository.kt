package com.uade.dda2.server.feature.enrollmentperiod.repository

import com.uade.dda2.server.feature.enrollmentperiod.entity.EnrollmentPeriod
import com.uade.dda2.server.feature.enrollmentperiod.entity.EnrollmentPeriodStatus
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate
import java.util.UUID

interface EnrollmentPeriodRepository : JpaRepository<EnrollmentPeriod, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
            SELECT ep
            FROM EnrollmentPeriod ep
            JOIN FETCH ep.programEdition e
            JOIN FETCH e.program
            WHERE ep.id = :id
        """,
    )
    fun findByIdForUpdate(
        @Param("id") id: UUID,
    ): EnrollmentPeriod?

    fun findAllByProgramEditionIdOrderByOpenDateDesc(
        programEditionId: UUID,
        pageable: Pageable,
    ): Page<EnrollmentPeriod>

    fun existsByProgramEditionId(programEditionId: UUID): Boolean

    fun existsByProgramEditionIdAndStatus(
        programEditionId: UUID,
        status: EnrollmentPeriodStatus,
    ): Boolean

    fun existsByProgramEditionIdAndStatusAndIdNot(
        programEditionId: UUID,
        status: EnrollmentPeriodStatus,
        id: UUID,
    ): Boolean

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
            SELECT ep
            FROM EnrollmentPeriod ep
            WHERE ep.status IN :statuses
              AND ep.closeDate < :currentDate
            ORDER BY ep.closeDate ASC, ep.id ASC
        """,
    )
    fun findExpiredForUpdate(
        @Param("statuses") statuses: Collection<EnrollmentPeriodStatus>,
        @Param("currentDate") currentDate: LocalDate,
    ): List<EnrollmentPeriod>

    fun findAllByProgramEditionIdInAndStatusAndOpenDateLessThanEqualAndCloseDateGreaterThanEqualOrderByOpenDateAsc(
        programEditionIds: Collection<UUID>,
        status: EnrollmentPeriodStatus,
        openDate: LocalDate,
        closeDate: LocalDate,
    ): List<EnrollmentPeriod>

    @Query(
        """
            SELECT CASE WHEN COUNT(ep) > 0 THEN true ELSE false END
            FROM EnrollmentPeriod ep
            WHERE ep.programEdition.id = :programEditionId
              AND ep.openDate <= :closeDate
              AND ep.closeDate >= :openDate
        """,
    )
    fun existsOverlapping(
        @Param("programEditionId") programEditionId: UUID,
        @Param("openDate") openDate: LocalDate,
        @Param("closeDate") closeDate: LocalDate,
    ): Boolean

    @Query(
        """
            SELECT CASE WHEN COUNT(ep) > 0 THEN true ELSE false END
            FROM EnrollmentPeriod ep
            WHERE ep.programEdition.id = :programEditionId
              AND ep.id <> :excludedId
              AND ep.openDate <= :closeDate
              AND ep.closeDate >= :openDate
        """,
    )
    fun existsOverlappingExcluding(
        @Param("programEditionId") programEditionId: UUID,
        @Param("excludedId") excludedId: UUID,
        @Param("openDate") openDate: LocalDate,
        @Param("closeDate") closeDate: LocalDate,
    ): Boolean

    @Query(
        """
            SELECT CASE WHEN COUNT(ep) > 0 THEN true ELSE false END
            FROM EnrollmentPeriod ep
            WHERE ep.programEdition.id = :programEditionId
              AND (ep.openDate < :startDate OR ep.closeDate > :endDate)
        """,
    )
    fun existsOutsideDateRange(
        @Param("programEditionId") programEditionId: UUID,
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate,
    ): Boolean
}
