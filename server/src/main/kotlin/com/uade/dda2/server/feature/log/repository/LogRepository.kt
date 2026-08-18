package com.uade.dda2.server.feature.log.repository

import com.uade.dda2.server.feature.log.entity.Log
import com.uade.dda2.server.feature.log.entity.LogEntityType
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface LogRepository : JpaRepository<Log, Long> {
    fun findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
        entityType: LogEntityType,
        entityId: String,
    ): List<Log>

    fun findByUserIdOrderByCreatedAtDesc(userId: Long): List<Log>

    @Modifying
    @Query("update Log l set l.user = null where l.user.id = :userId")
    fun clearUserReference(userId: Long): Int
}
