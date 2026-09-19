package com.uade.dda2.server.feature.center.repository

import com.uade.dda2.server.feature.center.entity.CenterService
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface CenterServiceRepository : JpaRepository<CenterService, UUID> {
    fun findByCenterIdAndServiceId(centerId: UUID, serviceId: UUID): CenterService?

    fun findAllByCenterIdAndActive(centerId: UUID, active: Boolean): List<CenterService>

    fun findAllByServiceIdAndActive(serviceId: UUID, active: Boolean): List<CenterService>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select cs from CenterService cs where cs.id = :id")
    fun findByIdForUpdate(@Param("id") id: UUID): CenterService?
}
