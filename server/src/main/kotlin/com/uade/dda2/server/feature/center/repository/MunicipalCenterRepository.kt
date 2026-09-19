package com.uade.dda2.server.feature.center.repository

import com.uade.dda2.server.feature.center.entity.MunicipalCenter
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface MunicipalCenterRepository : JpaRepository<MunicipalCenter, UUID> {
    fun existsByNormalizedName(normalizedName: String): Boolean

    fun existsByNormalizedNameAndIdNot(normalizedName: String, id: UUID): Boolean

    fun findAllByActiveOrderByNameAsc(active: Boolean): List<MunicipalCenter>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from MunicipalCenter c where c.id = :id")
    fun findByIdForUpdate(@Param("id") id: UUID): MunicipalCenter?
}
