package com.uade.dda2.server.feature.center.repository

import com.uade.dda2.server.feature.center.entity.MunicipalService
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface MunicipalServiceRepository : JpaRepository<MunicipalService, UUID> {
    fun existsByNormalizedName(normalizedName: String): Boolean

    fun existsByNormalizedNameAndIdNot(normalizedName: String, id: UUID): Boolean

    fun findAllByActiveOrderByNameAsc(active: Boolean): List<MunicipalService>

    @Query(
        """
        select distinct service from ProfessionalAvailability availability
        join availability.assignment assignment
        join assignment.professional professional
        join professional.roles professionalRole
        join assignment.centerService centerService
        join centerService.center center
        join centerService.service service
        where availability.active = true
          and assignment.active = true
          and professional.active = true
          and upper(professionalRole.name) = 'PROFESIONAL_CENTRO'
          and centerService.active = true
          and center.active = true
          and service.active = true
        order by service.name
        """,
    )
    fun findEffectiveForAppointments(): List<MunicipalService>

    @Query(
        """
        select s from MunicipalService s
        where (:search is null or s.normalizedName like concat('%', :search, '%'))
          and (:active is null or s.active = :active)
        order by s.name asc
        """,
    )
    fun search(
        @Param("search") search: String?,
        @Param("active") active: Boolean?,
        pageable: Pageable,
    ): Page<MunicipalService>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from MunicipalService s where s.id = :id")
    fun findByIdForUpdate(@Param("id") id: UUID): MunicipalService?
}
