package com.uade.dda2.server.feature.intervention.entity

import jakarta.persistence.*
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "intervention_action", uniqueConstraints = [
    UniqueConstraint(name = "uk_intervention_action_position", columnNames = ["objective_id", "position"]),
], check = [
    CheckConstraint(name = "ck_intervention_action_position", constraint = "position >= 0"),
    CheckConstraint(name = "ck_intervention_action_description", constraint = "length(trim(description)) > 0"),
    CheckConstraint(name = "ck_intervention_action_dates", constraint = "start_date <= due_date"),
    CheckConstraint(name = "ck_intervention_action_status", constraint = "status in ('PENDING', 'IN_PROGRESS', 'COMPLETED')"),
])
class InterventionAction(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "objective_id", nullable = false, updatable = false)
    val objective: InterventionObjective,
    @Column(nullable = false, updatable = false)
    val position: Int,
    @Column(nullable = false, length = 1000, updatable = false)
    val description: String,
    @Column(name = "start_date", nullable = false, updatable = false)
    val startDate: LocalDate,
    @Column(name = "due_date", nullable = false, updatable = false)
    val dueDate: LocalDate,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    var status: InterventionActionStatus = InterventionActionStatus.PENDING,
)
