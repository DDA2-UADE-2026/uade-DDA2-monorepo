package com.uade.dda2.server.feature.auth.repository

import com.uade.dda2.server.feature.auth.entity.CitizenSnapshot
import org.springframework.data.jpa.repository.JpaRepository

interface CitizenSnapshotRepository : JpaRepository<CitizenSnapshot, Long>
