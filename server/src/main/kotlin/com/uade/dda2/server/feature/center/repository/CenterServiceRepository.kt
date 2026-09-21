package com.uade.dda2.server.feature.center.repository

import com.uade.dda2.server.feature.center.entity.CenterService
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface CenterServiceRepository : JpaRepository<CenterService, UUID> {
    fun findByCenterIdAndServiceId(centerId: UUID, serviceId: UUID): CenterService?

    fun findAllByCenterIdAndActive(centerId: UUID, active: Boolean): List<CenterService>

    fun findAllByServiceIdAndActive(serviceId: UUID, active: Boolean): List<CenterService>

    @Query(
        """
        select distinct centerService from ProfessionalAvailability availability
        join availability.assignment assignment
        join assignment.professional professional
        join professional.roles professionalRole
        join assignment.centerService centerService
        join fetch centerService.center center
        join fetch centerService.service service
        where service.id = :serviceId
          and availability.active = true
          and assignment.active = true
          and professional.active = true
          and upper(professionalRole.name) = 'PROFESIONAL_CENTRO'
          and centerService.active = true
          and center.active = true
          and service.active = true
        order by center.name
        """,
    )
    fun findEffectiveByServiceId(@Param("serviceId") serviceId: UUID): List<CenterService>

    @EntityGraph(attributePaths = ["center", "service"])
    fun findAllByCenterIdOrderByServiceNameAsc(centerId: UUID): List<CenterService>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select cs from CenterService cs join fetch cs.center join fetch cs.service where cs.id = :id")
    fun findByIdForUpdate(@Param("id") id: UUID): CenterService?
}
