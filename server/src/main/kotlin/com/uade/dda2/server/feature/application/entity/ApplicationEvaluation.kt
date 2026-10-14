package com.uade.dda2.server.feature.application.entity

import com.uade.dda2.server.feature.auth.entity.User
import jakarta.persistence.CheckConstraint
import jakarta.persistence.Column
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
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(
    name = "application_evaluation",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_application_evaluation_application", columnNames = ["application_id"]),
    ],
    indexes = [
        Index(name = "ix_application_evaluation_professional", columnList = "professional_user_id"),
        Index(name = "ix_application_evaluation_evaluated_at", columnList = "evaluated_at"),
    ],
    check = [
        CheckConstraint(
            name = "ck_application_evaluation_vulnerability",
            constraint = "vulnerability in ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')",
        ),
        CheckConstraint(
            name = "ck_application_evaluation_priority",
            constraint = "priority in ('LOW', 'MEDIUM', 'HIGH', 'URGENT')",
        ),
    ],
)
class ApplicationEvaluation(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false, updatable = false)
    var application: Application,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    var vulnerability: VulnerabilityLevel,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    var priority: EvaluationPriority,

    @Column(nullable = false, length = 1000, updatable = false)
    var recommendation: String,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "professional_user_id", nullable = false, updatable = false)
    var professional: User,

    @Column(name = "evaluated_at", nullable = false, updatable = false)
    var evaluatedAt: OffsetDateTime,
)
