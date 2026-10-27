package com.uade.dda2.server.feature.intervention.entity

import jakarta.persistence.*
import org.hibernate.annotations.Immutable
import java.util.UUID

@Entity
@Immutable
@Table(name = "intervention_objective", uniqueConstraints = [
    UniqueConstraint(name = "uk_intervention_objective_position", columnNames = ["plan_id", "position"]),
], check = [
    CheckConstraint(name = "ck_intervention_objective_position", constraint = "position >= 0"),
    CheckConstraint(name = "ck_intervention_objective_description", constraint = "length(trim(description)) > 0"),
])
class InterventionObjective(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false, updatable = false)
    val plan: InterventionPlan,
    @Column(nullable = false, updatable = false)
    val position: Int,
    @Column(nullable = false, length = 1000, updatable = false)
    val description: String,
) {
    @OneToMany(mappedBy = "objective", cascade = [CascadeType.PERSIST])
    @OrderBy("position ASC")
    val actions: MutableList<InterventionAction> = mutableListOf()
}
