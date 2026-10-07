package com.uade.dda2.server.feature.appointment.repository

import com.uade.dda2.server.feature.appointment.entity.CommunityAttention
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface CommunityAttentionRepository : JpaRepository<CommunityAttention, UUID> {
    fun findByAppointmentId(appointmentId: UUID): CommunityAttention?

    fun findAllByAppointmentIdIn(appointmentIds: Collection<UUID>): List<CommunityAttention>
}
