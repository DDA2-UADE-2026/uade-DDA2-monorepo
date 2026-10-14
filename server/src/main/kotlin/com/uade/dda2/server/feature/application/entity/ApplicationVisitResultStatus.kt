package com.uade.dda2.server.feature.application.entity

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Resultado final admitido para una visita o entrevista social.")
enum class ApplicationVisitResultStatus {
    REALIZED,
    NOT_REALIZED,
}
