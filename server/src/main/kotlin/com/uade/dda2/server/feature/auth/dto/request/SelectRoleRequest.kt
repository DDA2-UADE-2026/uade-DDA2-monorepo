package com.uade.dda2.server.feature.auth.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "Selección de rol luego de un login con múltiples roles. No acepta un JWT operativo.")
data class SelectRoleRequest(
    @field:Schema(description = "JWT temporal recibido en selectionToken. Se envía en el body, no como bearer.", accessMode = Schema.AccessMode.WRITE_ONLY)
    @field:NotBlank
    val selectionToken: String = "",

    @field:Schema(description = "Nombre exacto de uno de los roles devueltos por el login.", example = "AUDITOR")
    @field:NotBlank
    @field:Size(max = 50)
    val role: String = "",
)
