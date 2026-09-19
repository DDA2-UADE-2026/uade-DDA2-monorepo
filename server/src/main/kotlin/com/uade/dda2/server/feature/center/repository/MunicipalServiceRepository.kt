package com.uade.dda2.server.feature.center.repository

import com.uade.dda2.server.feature.center.entity.MunicipalService
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface MunicipalServiceRepository : JpaRepository<MunicipalService, UUID> {
    fun existsByNormalizedName(normalizedName: String): Boolean

    fun existsByNormalizedNameAndIdNot(normalizedName: String, id: UUID): Boolean

    fun findAllByActiveOrderByNameAsc(active: Boolean): List<MunicipalService>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from MunicipalService s where s.id = :id")
    fun findByIdForUpdate(@Param("id") id: UUID): MunicipalService?
}
