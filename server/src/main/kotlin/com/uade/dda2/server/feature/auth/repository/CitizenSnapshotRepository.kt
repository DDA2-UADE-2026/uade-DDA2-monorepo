package com.uade.dda2.server.feature.auth.repository

import com.uade.dda2.server.feature.auth.entity.CitizenSnapshot
import org.springframework.data.jpa.repository.JpaRepository

interface CitizenSnapshotRepository : JpaRepository<CitizenSnapshot, Long> {
    @org.springframework.data.jpa.repository.Query("select s from CitizenSnapshot s where s.reviewRequired = true or s.blocked = true or s.deceasedAt is not null")
    fun findIntegrationAlerts(pageable: org.springframework.data.domain.Pageable): org.springframework.data.domain.Page<CitizenSnapshot>
}
