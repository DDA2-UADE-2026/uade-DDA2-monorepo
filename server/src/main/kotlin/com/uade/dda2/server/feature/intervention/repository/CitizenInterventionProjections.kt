package com.uade.dda2.server.feature.intervention.repository

import com.uade.dda2.server.feature.intervention.entity.InterventionActionStatus
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

interface CitizenInterventionPlanRow {
    val id: UUID
    val applicationId: UUID
    val applicationNumber: Long
    val programId: UUID
    val programName: String
    val programEditionId: UUID
    val programEditionName: String
    val createdAt: OffsetDateTime
}
interface InterventionPlanActionCounts {
    val planId: UUID
    val actionCount: Long
    val pendingCount: Long
    val completedCount: Long
}
interface CitizenInterventionActionRow {
    val objectiveId: UUID
    val objectiveDescription: String
    val id: UUID
    val description: String
    val startDate: LocalDate
    val dueDate: LocalDate
    val status: InterventionActionStatus
}
interface CitizenInterventionFollowUpRow {
    val id: UUID
    val actionId: UUID
    val occurredOn: LocalDate
    val status: InterventionActionStatus
    val summary: String
    val publishedAt: OffsetDateTime
}
