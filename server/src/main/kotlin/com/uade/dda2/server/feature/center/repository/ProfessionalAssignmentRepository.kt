package com.uade.dda2.server.feature.center.repository

import com.uade.dda2.server.feature.center.entity.ProfessionalAssignment
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface ProfessionalAssignmentRepository : JpaRepository<ProfessionalAssignment, UUID> {
    fun findByProfessionalIdAndCenterServiceId(
        professionalId: Long,
        centerServiceId: UUID,
    ): ProfessionalAssignment?

    fun findAllByCenterServiceIdAndActive(
        centerServiceId: UUID,
        active: Boolean,
    ): List<ProfessionalAssignment>

    fun findAllByProfessionalIdAndActive(
        professionalId: Long,
        active: Boolean,
    ): List<ProfessionalAssignment>

    @EntityGraph(attributePaths = ["professional", "centerService", "centerService.center", "centerService.service"])
    fun findAllByCenterServiceIdOrderByProfessionalNameAsc(centerServiceId: UUID): List<ProfessionalAssignment>

    fun existsByProfessionalId(professionalId: Long): Boolean

    fun existsByProfessionalIdAndActiveTrue(professionalId: Long): Boolean

    fun existsByActiveTrue(): Boolean

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
        select a from ProfessionalAssignment a
        join fetch a.professional
        join fetch a.centerService cs
        join fetch cs.center
        join fetch cs.service
        where a.id = :id
        """,
    )
    fun findByIdForUpdate(@Param("id") id: UUID): ProfessionalAssignment?
}
