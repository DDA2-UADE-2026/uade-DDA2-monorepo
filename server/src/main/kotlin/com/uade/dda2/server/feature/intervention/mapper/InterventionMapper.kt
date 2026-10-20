package com.uade.dda2.server.feature.intervention.mapper

import com.uade.dda2.server.feature.intervention.dto.response.*
import com.uade.dda2.server.feature.intervention.entity.*

fun InterventionPlan.toResponse() = InterventionPlanResponse(
    requireNotNull(id), requireNotNull(application.id), requireNotNull(application.user.id),
    requireNotNull(responsible.id), responsible.name, createdAt,
    objectives.map { objective -> InterventionObjectiveResponse(requireNotNull(objective.id), objective.description,
        objective.actions.map { action -> InterventionActionResponse(requireNotNull(action.id), action.description,
            action.startDate, action.dueDate, action.status, requireNotNull(responsible.id), responsible.name) }) },
)

fun InterventionFollowUp.toResponse(): InterventionFollowUpResponse {
    val plan = action.objective.plan
    return InterventionFollowUpResponse(requireNotNull(id), requireNotNull(plan.id), requireNotNull(plan.application.id),
        requireNotNull(plan.application.user.id), requireNotNull(action.objective.id), requireNotNull(action.id),
        sequenceNumber, previousStatus, status, occurredOn, notes, result, requireNotNull(author.id), author.name, recordedAt)
}

fun InterventionPlan.toAuditSnapshot(): Map<String, Any?> = mapOf(
    "id" to id, "applicationId" to application.id, "userId" to application.user.id,
    "responsibleUserId" to responsible.id, "createdAt" to createdAt,
    "objectives" to objectives.map { objective -> mapOf("id" to objective.id, "actions" to objective.actions.map {
        mapOf("id" to it.id, "status" to it.status, "startDate" to it.startDate, "dueDate" to it.dueDate) }) },
)

fun InterventionFollowUp.toAuditSnapshot(): Map<String, Any?> = mapOf(
    "id" to id, "planId" to action.objective.plan.id, "applicationId" to action.objective.plan.application.id,
    "actionId" to action.id, "sequenceNumber" to sequenceNumber, "previousStatus" to previousStatus, "status" to status,
    "occurredOn" to occurredOn, "authorUserId" to author.id, "recordedAt" to recordedAt,
)
