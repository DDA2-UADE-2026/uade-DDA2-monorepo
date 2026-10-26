package com.uade.dda2.server.feature.intervention.entity

import com.uade.dda2.server.feature.application.entity.Application
import com.uade.dda2.server.feature.auth.entity.User
import jakarta.persistence.*
import org.hibernate.annotations.Immutable
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Immutable
@Table(name = "intervention_plan", uniqueConstraints = [
    UniqueConstraint(name = "uk_intervention_plan_application", columnNames = ["application_id"]),
], indexes = [Index(name = "ix_intervention_plan_responsible", columnList = "responsible_user_id")])
class InterventionPlan(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false, updatable = false)
    val application: Application,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "responsible_user_id", nullable = false, updatable = false)
    val responsible: User,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: OffsetDateTime,
) {
    @OneToMany(mappedBy = "plan", cascade = [CascadeType.PERSIST])
    @OrderBy("position ASC")
    val objectives: MutableList<InterventionObjective> = mutableListOf()
}
