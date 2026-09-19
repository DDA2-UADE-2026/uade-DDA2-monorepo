package com.uade.dda2.server.feature.center.repository

import com.uade.dda2.server.feature.center.entity.ProfessionalAvailability
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID

interface ProfessionalAvailabilityRepository : JpaRepository<ProfessionalAvailability, UUID> {
    fun findAllByAssignmentIdAndActiveOrderByDayOfWeekAscStartTimeAsc(
        assignmentId: UUID,
        active: Boolean,
    ): List<ProfessionalAvailability>

    @Query(
        """
        select availability from ProfessionalAvailability availability
        join fetch availability.assignment assignment
        join fetch assignment.professional professional
        join fetch assignment.centerService centerService
        join fetch centerService.center center
        join fetch centerService.service service
        where center.id = :centerId
          and availability.dayOfWeek = :dayOfWeek
          and availability.active = true
          and assignment.active = true
          and professional.active = true
          and centerService.active = true
          and center.active = true
          and service.active = true
        order by availability.startTime
        """,
    )
    fun findEffectiveByCenterIdAndDayOfWeek(
        @Param("centerId") centerId: UUID,
        @Param("dayOfWeek") dayOfWeek: DayOfWeek,
    ): List<ProfessionalAvailability>

    @Query(
        """
        select availability from ProfessionalAvailability availability
        join availability.assignment assignment
        join assignment.professional professional
        join assignment.centerService centerService
        join centerService.center center
        join centerService.service service
        where professional.id = :professionalId
          and availability.dayOfWeek = :dayOfWeek
          and availability.active = true
          and assignment.active = true
          and professional.active = true
          and centerService.active = true
          and center.active = true
          and service.active = true
          and availability.startTime < :requestedEnd
          and availability.endTime > :requestedStart
        order by availability.startTime
        """,
    )
    fun findEffectiveOverlapsByProfessionalId(
        @Param("professionalId") professionalId: Long,
        @Param("dayOfWeek") dayOfWeek: DayOfWeek,
        @Param("requestedStart") requestedStart: LocalTime,
        @Param("requestedEnd") requestedEnd: LocalTime,
    ): List<ProfessionalAvailability>

    @Query(
        """
        select availability from ProfessionalAvailability availability
        join availability.assignment assignment
        join assignment.professional professional
        join assignment.centerService centerService
        join centerService.center center
        join centerService.service service
        where professional.id = :professionalId
          and availability.dayOfWeek = :dayOfWeek
          and availability.active = true
          and assignment.active = true
          and professional.active = true
          and centerService.active = true
          and center.active = true
          and service.active = true
          and availability.id <> :excludedId
          and availability.startTime < :requestedEnd
          and availability.endTime > :requestedStart
        order by availability.startTime
        """,
    )
    fun findEffectiveOverlapsByProfessionalIdExcludingId(
        @Param("professionalId") professionalId: Long,
        @Param("dayOfWeek") dayOfWeek: DayOfWeek,
        @Param("requestedStart") requestedStart: LocalTime,
        @Param("requestedEnd") requestedEnd: LocalTime,
        @Param("excludedId") excludedId: UUID,
    ): List<ProfessionalAvailability>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select availability from ProfessionalAvailability availability where availability.id = :id")
    fun findByIdForUpdate(@Param("id") id: UUID): ProfessionalAvailability?
}
