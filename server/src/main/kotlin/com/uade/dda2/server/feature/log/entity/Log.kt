package com.uade.dda2.server.feature.log.entity

import com.uade.dda2.server.feature.auth.entity.User
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.ColumnTransformer
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Entity
@Table(
    name = "logs",
    indexes = [
        Index(name = "ix_logs_entity", columnList = "entity_type, entity_id"),
        Index(name = "ix_logs_user_id", columnList = "user_id"),
        Index(name = "ix_logs_created_at", columnList = "created_at"),
    ],
)
class Log(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "user_id")
    val user: User? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 20)
    val action: LogAction,

    @Convert(converter = LogEntityTypeConverter::class)
    @Column(name = "entity_type", nullable = false, length = 100)
    val entityType: LogEntityType,

    @Column(name = "entity_id", nullable = false, length = 255)
    val entityId: String,

    @Column(name = "old_values", columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    val oldValues: String? = null,

    @Column(name = "new_values", columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    val newValues: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: OffsetDateTime = OffsetDateTime.now(ZoneOffset.UTC),
)
