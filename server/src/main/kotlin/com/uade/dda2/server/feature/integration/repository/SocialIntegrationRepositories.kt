package com.uade.dda2.server.feature.integration.repository

import com.uade.dda2.server.feature.integration.entity.*
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface SocialTicketRepository : JpaRepository<SocialTicket, UUID> {
    @Query("select t from SocialTicket t where (:state is null or t.state = :state) and (:attention is null or t.requiresAttention = :attention)")
    fun findAdministrativePage(state: SocialTicketState?, attention: Boolean?, pageable: Pageable): Page<SocialTicket>
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from SocialTicket t where t.id = :id")
    fun lockById(id: UUID): SocialTicket?
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from SocialTicket t where t.applicationId = :applicationId")
    fun lockByApplicationId(applicationId: UUID): SocialTicket?
    fun findAllByState(state: SocialTicketState, pageable: Pageable): Page<SocialTicket>
}

interface SocialTicketCursorRepository : JpaRepository<SocialTicketCursor, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from SocialTicketCursor c where c.id = :id")
    fun lockById(id: UUID): SocialTicketCursor?
}

interface RevenueDebtRepository : JpaRepository<RevenueDebt, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from RevenueDebt d where d.id = :id")
    fun lockById(id: UUID): RevenueDebt?
    fun findAllByCitizenId(citizenId: UUID, pageable: Pageable): Page<RevenueDebt>
}

interface RevenueExemptionRepository : JpaRepository<RevenueExemption, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from RevenueExemption e where e.id = :id")
    fun lockById(id: UUID): RevenueExemption?
    fun findAllByCitizenId(citizenId: UUID, pageable: Pageable): Page<RevenueExemption>
}

interface ExternalDocumentationReceiptRepository : JpaRepository<ExternalDocumentationReceipt, UUID> {
    fun findAllByApplicationId(applicationId: UUID): List<ExternalDocumentationReceipt>
    fun findAllByApplicationIdIn(applicationIds: Collection<UUID>): List<ExternalDocumentationReceipt>
}
