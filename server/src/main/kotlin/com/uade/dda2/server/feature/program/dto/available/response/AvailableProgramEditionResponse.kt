package com.uade.dda2.server.feature.program.dto.available.response

import com.uade.dda2.server.feature.program.entity.enums.ProgramEditionStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.util.UUID

@Schema(description = "Edición disponible de un programa, con sus beneficios y requisitos.")
data class AvailableProgramEditionResponse(
    @field:Schema(description = "UUID de la edición.", format = "uuid", accessMode = Schema.AccessMode.READ_ONLY)
    val id: UUID,
    @field:Schema(description = "Nombre de la edición.", example = "Convocatoria 2026", accessMode = Schema.AccessMode.READ_ONLY)
    val name: String,
    @field:Schema(description = "Fecha de inicio.", example = "2026-09-01", format = "date", accessMode = Schema.AccessMode.READ_ONLY)
    val startDate: LocalDate,
    @field:Schema(description = "Fecha de finalización.", example = "2026-12-15", format = "date", accessMode = Schema.AccessMode.READ_ONLY)
    val endDate: LocalDate,
    @field:Schema(description = "Capacidad máxima de participantes.", example = "250", accessMode = Schema.AccessMode.READ_ONLY)
    val maxCapacity: Int,
    @field:Schema(description = "Cantidad actual de participantes inscriptos.", example = "87", accessMode = Schema.AccessMode.READ_ONLY)
    val currentEnrollment: Int,
    @field:Schema(description = "Cantidad de vacantes disponibles.", example = "163", accessMode = Schema.AccessMode.READ_ONLY)
    val availableCapacity: Int,
    @field:Schema(description = "Estado actual de la edición.", example = "ACTIVE", accessMode = Schema.AccessMode.READ_ONLY)
    val status: ProgramEditionStatus,
    @field:Schema(description = "Beneficios ofrecidos por esta edición.", accessMode = Schema.AccessMode.READ_ONLY)
    val benefits: List<AvailableProgramBenefitResponse>,
    @field:Schema(description = "Requisitos exigidos por esta edición.", accessMode = Schema.AccessMode.READ_ONLY)
    val requirements: List<AvailableProgramRequirementResponse>,
    @field:Schema(description = "Períodos actualmente abiertos para recibir solicitudes.", accessMode = Schema.AccessMode.READ_ONLY)
    val enrollmentPeriods: List<AvailableEnrollmentPeriodResponse>,
)
