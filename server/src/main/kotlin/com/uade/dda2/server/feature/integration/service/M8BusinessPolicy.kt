package com.uade.dda2.server.feature.integration.service

import com.uade.dda2.server.config.CoreProperties
import com.uade.dda2.server.error.ConflictException
import com.uade.dda2.server.feature.auth.entity.User
import com.uade.dda2.server.feature.auth.repository.CitizenSnapshotRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class M8BusinessPolicy(private val core: CoreProperties, private val snapshots: CitizenSnapshotRepository) {
    fun enabled(): Boolean = core.enabled && core.business.enabled

    fun citizenId(user: User): UUID {
        val raw = user.externalCitizenId
        if (raw == null || !raw.matches(Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")))
            throw ConflictException("M8_CITIZEN_LINK_REQUIRED", "La operación requiere vincular previamente al usuario con el UUID canónico de Ciudadanos.")
        return UUID.fromString(raw)
    }

    fun validateNewApplication(user: User) {
        if (!enabled()) return
        val snapshot = snapshots.findById(requireNotNull(user.id)).orElse(null)
        if (snapshot?.blocked == true || snapshot?.deceasedAt != null)
            throw ConflictException("M8_CITIZEN_APPLICATION_BLOCKED", "Ciudadanos informó una restricción que impide nuevas solicitudes para este titular.")
    }
}
