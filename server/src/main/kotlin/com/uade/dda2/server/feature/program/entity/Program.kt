package com.uade.dda2.server.feature.program.entity

import com.uade.dda2.server.feature.auth.entity.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "program",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_program_normalized_name",
            columnNames = ["normalized_name"],
        ),
    ],
)
class Program(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(
        nullable = false,
        length = 200,
    )
    var name: String,

    @Column(
        name = "normalized_name",
        nullable = false,
        length = 200,
    )
    var normalizedName: String = name.trim().lowercase(),

    @Column(
        columnDefinition = "TEXT",
    )
    var objective: String? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    var createdBy: User,

    @Column(
        name = "created_at",
        nullable = false,
        updatable = false,
    )
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(
        name = "updated_at",
        nullable = false,
    )
    var updatedAt: LocalDateTime = LocalDateTime.now(),
) {

    @PrePersist
    fun prePersist() {
        val now = LocalDateTime.now()

        normalizeName()
        createdAt = now
        updatedAt = now
    }

    @PreUpdate
    fun preUpdate() {
        normalizeName()
        updatedAt = LocalDateTime.now()
    }

    private fun normalizeName() {
        name = name.trim()
        normalizedName = name.lowercase()
    }
}
