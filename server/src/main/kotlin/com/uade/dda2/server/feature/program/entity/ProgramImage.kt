package com.uade.dda2.server.feature.program.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "program_image",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_program_image_program",
            columnNames = ["program_id"],
        ),
    ],
)
class ProgramImage(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "program_id", nullable = false, unique = true, updatable = false)
    var program: Program,

    @Column(name = "original_name", nullable = false, length = 255)
    var originalName: String,

    @Column(name = "content_type", nullable = false, length = 100)
    var contentType: String,

    @Column(name = "size_bytes", nullable = false)
    var sizeBytes: Long,

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "content", nullable = false, columnDefinition = "bytea")
    var content: ByteArray,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
)
