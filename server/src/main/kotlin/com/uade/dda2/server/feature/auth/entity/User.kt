package com.uade.dda2.server.feature.auth.entity

import jakarta.persistence.CheckConstraint
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(
    name = "users",
    check = [
        CheckConstraint(
            name = "ck_users_local_credentials",
            constraint = "(username is null and password_hash is null) or (username is not null and password_hash is not null)",
        ),
    ],
)
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(unique = true, length = 80)
    var username: String? = null,

    @Column(name = "password_hash", length = 100)
    var passwordHash: String? = null,

    // Reserved for a future flow that verifies control of both identities.
    // This is not a foreign key to Ciudadanos and is not writable through user DTOs.
    @Column(name = "external_citizen_id", unique = true, length = 255)
    var externalCitizenId: String? = null,

    @Column(nullable = false, length = 150)
    var name: String = "",

    @Column(nullable = false, length = 180)
    var email: String = "",

    @Column(nullable = false)
    var active: Boolean = true,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at")
    var updatedAt: Instant? = null,

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_roles",
        joinColumns = [JoinColumn(name = "user_id")],
        inverseJoinColumns = [JoinColumn(name = "role_id")],
    )
    var roles: MutableSet<Role> = mutableSetOf(),
)
