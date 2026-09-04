package com.uade.dda2.server.feature.auth.repository

import com.uade.dda2.server.feature.auth.entity.User
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.Lock
import jakarta.persistence.LockModeType

interface UserRepository : JpaRepository<User, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    fun findByIdForUpdate(id: Long): User?

    @EntityGraph(attributePaths = ["roles", "roles.permissions"])
    fun findAllByOrderByUsernameAsc(): List<User>

    @EntityGraph(attributePaths = ["roles", "roles.permissions"])
    fun findByUsernameIgnoreCase(username: String): User?

    @EntityGraph(attributePaths = ["roles", "roles.permissions"])
    fun findByExternalCitizenId(externalCitizenId: String): User?

    @EntityGraph(attributePaths = ["roles", "roles.permissions"])
    @Query("select u from User u where u.id = :id")
    fun findByIdWithRoles(id: Long): User?

    @EntityGraph(attributePaths = ["roles"])
    fun findByRolesId(roleId: Long): List<User>

    fun existsByUsernameIgnoreCase(username: String): Boolean

    fun existsByUsernameIgnoreCaseAndIdNot(username: String, id: Long): Boolean
}
