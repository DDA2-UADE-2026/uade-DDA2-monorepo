package com.uade.dda2.server.feature.document.entity

import com.uade.dda2.server.feature.auth.entity.User
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "document", indexes = [
    Index(name = "ix_document_uploaded_by", columnList = "uploaded_by_user_id"),
])
class Document(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(name = "original_name", nullable = false, length = 255, updatable = false)
    var originalName: String,

    @Column(name = "content_type", nullable = false, length = 100, updatable = false)
    var contentType: String,

    @Column(name = "size_bytes", nullable = false, updatable = false)
    var sizeBytes: Long,

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "content", nullable = false, updatable = false, columnDefinition = "bytea")
    var content: ByteArray,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by_user_id", nullable = false, updatable = false)
    var uploadedBy: User,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime,
)
