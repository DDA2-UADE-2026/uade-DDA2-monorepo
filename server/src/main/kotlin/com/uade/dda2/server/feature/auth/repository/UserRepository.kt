package com.uade.dda2.server.feature.auth.repository

import com.uade.dda2.server.feature.auth.entity.User
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface UserRepository : JpaRepository<User, Long> {
    @EntityGraph(attributePaths = ["roles", "roles.permissions"])
    fun findAllByOrderByUsernameAsc(): List<User>

    @EntityGraph(attributePaths = ["roles", "roles.permissions"])
    fun findByUsernameIgnoreCase(username: String): User?

    @EntityGraph(attributePaths = ["roles", "roles.permissions"])
    @Query("select u from User u where u.id = :id")
    fun findByIdWithRoles(id: Long): User?

    @EntityGraph(attributePaths = ["roles"])
    fun findByRolesId(roleId: Long): List<User>

    fun existsByUsernameIgnoreCase(username: String): Boolean

    fun existsByUsernameIgnoreCaseAndIdNot(username: String, id: Long): Boolean
}
