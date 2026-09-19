package com.uade.dda2.server.feature.center.repository

import com.uade.dda2.server.feature.center.entity.CenterOpeningHour
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID

interface CenterOpeningHourRepository : JpaRepository<CenterOpeningHour, UUID> {
    fun findAllByCenterIdAndActiveOrderByDayOfWeekAscStartTimeAsc(
        centerId: UUID,
        active: Boolean,
    ): List<CenterOpeningHour>

    @Query(
        """
        select h from CenterOpeningHour h
        where h.center.id = :centerId
          and h.dayOfWeek = :dayOfWeek
          and h.active = true
          and h.startTime < :requestedEnd
          and h.endTime > :requestedStart
        order by h.startTime
        """,
    )
    fun findActiveOverlaps(
        @Param("centerId") centerId: UUID,
        @Param("dayOfWeek") dayOfWeek: DayOfWeek,
        @Param("requestedStart") requestedStart: LocalTime,
        @Param("requestedEnd") requestedEnd: LocalTime,
    ): List<CenterOpeningHour>

    @Query(
        """
        select h from CenterOpeningHour h
        where h.center.id = :centerId
          and h.dayOfWeek = :dayOfWeek
          and h.active = true
          and h.id <> :excludedId
          and h.startTime < :requestedEnd
          and h.endTime > :requestedStart
        order by h.startTime
        """,
    )
    fun findActiveOverlapsExcludingId(
        @Param("centerId") centerId: UUID,
        @Param("dayOfWeek") dayOfWeek: DayOfWeek,
        @Param("requestedStart") requestedStart: LocalTime,
        @Param("requestedEnd") requestedEnd: LocalTime,
        @Param("excludedId") excludedId: UUID,
    ): List<CenterOpeningHour>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from CenterOpeningHour h where h.id = :id")
    fun findByIdForUpdate(@Param("id") id: UUID): CenterOpeningHour?
}
