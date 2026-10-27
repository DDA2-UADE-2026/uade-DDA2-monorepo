package com.uade.dda2.server.feature.application.entity

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Prioridad de intervencion determinada durante la evaluacion social.")
enum class EvaluationPriority {
    LOW,
    MEDIUM,
    HIGH,
    URGENT,
}
