package com.uade.dda2.server.feature.benefit.dto.response

import com.uade.dda2.server.feature.benefit.entity.BenefitGrantStatus
import com.uade.dda2.server.feature.program.entity.enums.ProgramBenefitType
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Schema(description = "Beneficio otorgado al titular autenticado. No incluye responsables, motivos ni historial administrativo.")
data class CitizenBenefitGrantResponse(
    val id: UUID,
    val applicationId: UUID,
    val applicationNumber: Long,
    val programBenefitId: UUID,
    val programId: UUID,
    val programName: String,
    val programEditionId: UUID,
    val programEditionName: String,
    val benefitType: ProgramBenefitType,
    @field:Schema(description = "Descripcion copiada al otorgar, independiente de cambios del catalogo.", types = ["string", "null"])
    val description: String?,
    @field:Schema(description = "Monto copiado al otorgar; null cuando no corresponde importe.", types = ["number", "null"])
    val amount: BigDecimal?,
    @field:Schema(description = "Estado persistido actual. FINALIZED y EXPIRED tambien permanecen visibles.")
    val status: BenefitGrantStatus,
    @field:Schema(description = "Inicio inclusivo de vigencia.")
    val startDate: LocalDate,
    @field:Schema(description = "Fin inclusivo vigente; refleja las renovaciones confirmadas.")
    val endDate: LocalDate,
)

@Schema(description = "Pagina de todos los beneficios del titular, de cualquier solicitud y estado, ordenados por otorgamiento e ID descendentes.")
data class CitizenBenefitGrantListResponse(
    val content: List<CitizenBenefitGrantResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    @field:Schema(description = "Mensaje informativo cuando totalElements es cero; null en los demas casos, incluso paginas fuera de rango.", example = "No tenés beneficios asignados.", types = ["string", "null"])
    val message: String?,
)
