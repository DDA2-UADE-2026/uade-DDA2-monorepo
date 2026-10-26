package com.uade.dda2.server.feature.intervention.entity

import com.uade.dda2.server.feature.auth.entity.User
import jakarta.persistence.*
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "intervention_follow_up_publication", uniqueConstraints = [
    UniqueConstraint(name = "uk_intervention_publication_follow_up", columnNames = ["follow_up_id"]),
], indexes = [Index(name = "ix_intervention_publication_updated_by", columnList = "updated_by_user_id")], check = [
    CheckConstraint(name = "ck_intervention_publication_summary", constraint = "length(trim(summary)) > 0"),
    CheckConstraint(name = "ck_intervention_publication_dates", constraint = "published_at <= updated_at"),
])
class InterventionFollowUpPublication(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "follow_up_id", nullable = false, updatable = false)
    val followUp: InterventionFollowUp,
    @Column(nullable = false, length = 2000)
    var summary: String,
    @Column(nullable = false)
    var visible: Boolean = true,
    @Column(name = "published_at", nullable = false)
    var publishedAt: OffsetDateTime,
    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "updated_by_user_id", nullable = false)
    var updatedBy: User,
)
