package com.uade.dda2.server.feature.auth.dto

import java.util.UUID

/** Resultado interno del aprovisionamiento. No concede una sesión ni transporta roles o credenciales. */
data class M1LocalAccount(val userId: Long, val citizenId: UUID, val created: Boolean)
