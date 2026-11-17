package com.uade.dda2.server.feature.integration.dto

import java.util.UUID

/** Credenciales salientes hacia M1. No registrar ni conservar la instancia. */
data class M1LoginRequest(val gmail: String, val password: String) {
    override fun toString(): String = "M1LoginRequest(<redacted>)"
}

/** Identidad aceptada desde el login HTTPS de M1; no contiene tokens ni permisos remotos. */
data class M1CitizenIdentity(
    val citizenId: UUID,
    val userId: Long?,
    val name: String,
)
