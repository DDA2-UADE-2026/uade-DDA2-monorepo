package com.uade.dda2.server.feature.auth.repository

import com.uade.dda2.server.feature.auth.entity.Permission
import org.springframework.data.jpa.repository.JpaRepository

interface PermissionRepository : JpaRepository<Permission, Long> {
    fun findAllByOrderByNameAsc(): List<Permission>

    fun findByNameIn(names: Collection<String>): List<Permission>
}
