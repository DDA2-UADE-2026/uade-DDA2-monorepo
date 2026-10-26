package com.uade.dda2.server.feature.intervention.mapper

import com.uade.dda2.server.feature.intervention.dto.response.*
import com.uade.dda2.server.feature.intervention.entity.InterventionActionStatus
import com.uade.dda2.server.feature.intervention.repository.*

fun interventionProgress(total: Long, pending: Long, completed: Long): InterventionActionStatus = when {
    total == 0L || total == pending -> InterventionActionStatus.PENDING
    total == completed -> InterventionActionStatus.COMPLETED
    else -> InterventionActionStatus.IN_PROGRESS
}

fun CitizenInterventionPlanRow.toCitizenSummary(status: InterventionActionStatus) = CitizenInterventionPlanSummaryResponse(
    id, applicationId, applicationNumber, programId, programName, programEditionId, programEditionName, createdAt, status,
)

fun CitizenInterventionPlanRow.toCitizenDetail(rows: List<CitizenInterventionActionRow>): CitizenInterventionPlanResponse {
    val status = interventionProgress(rows.size.toLong(), rows.count { it.status == InterventionActionStatus.PENDING }.toLong(),
        rows.count { it.status == InterventionActionStatus.COMPLETED }.toLong())
    return CitizenInterventionPlanResponse(id, applicationId, applicationNumber, programId, programName, programEditionId, programEditionName,
        createdAt, status, rows.groupBy { it.objectiveId }.map { (objectiveId, actions) ->
            CitizenInterventionObjectiveResponse(objectiveId, actions.first().objectiveDescription, actions.map {
                CitizenInterventionActionResponse(it.id, it.description, it.startDate, it.dueDate, it.status)
            })
        })
}

fun CitizenInterventionFollowUpRow.toCitizenResponse() = CitizenInterventionFollowUpResponse(id, actionId, occurredOn, status, summary, publishedAt)
