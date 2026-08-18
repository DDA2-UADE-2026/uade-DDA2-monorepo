package com.uade.dda2.server.feature.auth.repository

import com.uade.dda2.server.feature.auth.entity.Role
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface RoleRepository : JpaRepository<Role, Long> {
    @EntityGraph(attributePaths = ["permissions"])
    fun findAllByOrderByNameAsc(): List<Role>

    @EntityGraph(attributePaths = ["permissions"])
    @Query("select r from Role r where r.id = :id")
    fun findByIdWithPermissions(id: Long): Role?

    fun findByNameIn(names: Collection<String>): List<Role>

    fun existsByNameIgnoreCase(name: String): Boolean

    fun existsByNameIgnoreCaseAndIdNot(name: String, id: Long): Boolean
}
