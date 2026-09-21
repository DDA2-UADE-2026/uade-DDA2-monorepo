package com.uade.dda2.server.feature.appointment.repository

import com.uade.dda2.server.feature.appointment.entity.Appointment
import com.uade.dda2.server.feature.appointment.entity.AppointmentStatus
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.OffsetDateTime
import java.util.UUID

interface AppointmentRepository : JpaRepository<Appointment, UUID> {
    @EntityGraph(
        attributePaths = [
            "citizen",
            "professionalAssignment.professional",
            "professionalAssignment.centerService.center",
            "professionalAssignment.centerService.service",
        ],
    )
    fun findByIdAndCitizenId(id: UUID, citizenId: Long): Appointment?

    @EntityGraph(
        attributePaths = [
            "citizen",
            "professionalAssignment.professional",
            "professionalAssignment.centerService.center",
            "professionalAssignment.centerService.service",
        ],
    )
    fun findByCitizenIdAndIdempotencyKey(citizenId: Long, idempotencyKey: String): Appointment?

    @Query(
        """
        select appointment from Appointment appointment
        where appointment.citizen.id = :citizenId
          and appointment.status = :status
          and appointment.startsAt < :endsAt
          and appointment.endsAt > :startsAt
        """,
    )
    fun findOverlapsByCitizenId(
        @Param("citizenId") citizenId: Long,
        @Param("startsAt") startsAt: OffsetDateTime,
        @Param("endsAt") endsAt: OffsetDateTime,
        @Param("status") status: AppointmentStatus = AppointmentStatus.CONFIRMED,
    ): List<Appointment>

    @Query(
        """
        select appointment from Appointment appointment
        join appointment.professionalAssignment assignment
        where assignment.professional.id = :professionalId
          and appointment.status = :status
          and appointment.startsAt < :endsAt
          and appointment.endsAt > :startsAt
        """,
    )
    fun findOverlapsByProfessionalId(
        @Param("professionalId") professionalId: Long,
        @Param("startsAt") startsAt: OffsetDateTime,
        @Param("endsAt") endsAt: OffsetDateTime,
        @Param("status") status: AppointmentStatus = AppointmentStatus.CONFIRMED,
    ): List<Appointment>

    @Query(
        """
        select appointment from Appointment appointment
        join fetch appointment.professionalAssignment assignment
        join fetch assignment.professional professional
        where professional.id in :professionalIds
          and appointment.status = :status
          and appointment.startsAt < :rangeEnd
          and appointment.endsAt > :rangeStart
        """,
    )
    fun findByProfessionalIdsInRange(
        @Param("professionalIds") professionalIds: Collection<Long>,
        @Param("rangeStart") rangeStart: OffsetDateTime,
        @Param("rangeEnd") rangeEnd: OffsetDateTime,
        @Param("status") status: AppointmentStatus = AppointmentStatus.CONFIRMED,
    ): List<Appointment>

    @Query(
        """
        select appointment from Appointment appointment
        where appointment.citizen.id = :citizenId
          and appointment.status = :status
          and appointment.startsAt < :rangeEnd
          and appointment.endsAt > :rangeStart
        """,
    )
    fun findByCitizenIdInRange(
        @Param("citizenId") citizenId: Long,
        @Param("rangeStart") rangeStart: OffsetDateTime,
        @Param("rangeEnd") rangeEnd: OffsetDateTime,
        @Param("status") status: AppointmentStatus = AppointmentStatus.CONFIRMED,
    ): List<Appointment>

    @Query(
        """
        select case when count(appointment) > 0 then true else false end
        from Appointment appointment
        where appointment.citizen.id = :userId
           or appointment.professionalAssignment.professional.id = :userId
        """,
    )
    fun existsByCitizenOrProfessionalId(@Param("userId") userId: Long): Boolean
}
