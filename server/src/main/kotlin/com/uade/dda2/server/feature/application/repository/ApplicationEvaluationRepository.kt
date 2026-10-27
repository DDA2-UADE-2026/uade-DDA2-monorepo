package com.uade.dda2.server.feature.application.repository

import com.uade.dda2.server.feature.application.entity.ApplicationEvaluation
import com.uade.dda2.server.feature.application.entity.EvaluationPriority
import com.uade.dda2.server.feature.application.entity.VulnerabilityLevel
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.OffsetDateTime
import java.util.UUID

interface ApplicationEvaluationRepository : JpaRepository<ApplicationEvaluation, UUID> {
    fun findByApplicationId(applicationId: UUID): ApplicationEvaluation?

    fun existsByApplicationId(applicationId: UUID): Boolean

    fun existsByProfessionalId(professionalId: Long): Boolean

    @EntityGraph(
        attributePaths = [
            "professional",
            "application",
            "application.user",
            "application.programEdition",
            "application.programEdition.program",
        ],
    )
    @Query(
        """
        select evaluation from ApplicationEvaluation evaluation
        where (:applicationId is null or evaluation.application.id = :applicationId)
          and (:applicationNumber is null or evaluation.application.applicationNumber = :applicationNumber)
          and (:applicantUserId is null or evaluation.application.user.id = :applicantUserId)
          and (:professionalId is null or evaluation.professional.id = :professionalId)
          and (:programId is null or evaluation.application.programEdition.program.id = :programId)
          and (:programEditionId is null or evaluation.application.programEdition.id = :programEditionId)
          and (:vulnerability is null or evaluation.vulnerability = :vulnerability)
          and (:priority is null or evaluation.priority = :priority)
          and (cast(:evaluatedFrom as OffsetDateTime) is null or evaluation.evaluatedAt >= :evaluatedFrom)
          and (cast(:evaluatedTo as OffsetDateTime) is null or evaluation.evaluatedAt <= :evaluatedTo)
        """,
    )
    fun search(
        @Param("applicationId") applicationId: UUID?,
        @Param("applicationNumber") applicationNumber: Long?,
        @Param("applicantUserId") applicantUserId: Long?,
        @Param("professionalId") professionalId: Long?,
        @Param("programId") programId: UUID?,
        @Param("programEditionId") programEditionId: UUID?,
        @Param("vulnerability") vulnerability: VulnerabilityLevel?,
        @Param("priority") priority: EvaluationPriority?,
        @Param("evaluatedFrom") evaluatedFrom: OffsetDateTime?,
        @Param("evaluatedTo") evaluatedTo: OffsetDateTime?,
        pageable: Pageable,
    ): Page<ApplicationEvaluation>

    @EntityGraph(
        attributePaths = [
            "professional",
            "application",
            "application.user",
            "application.programEdition",
            "application.programEdition.program",
            "application.enrollmentPeriod",
        ],
    )
    @Query("select evaluation from ApplicationEvaluation evaluation where evaluation.id = :id")
    fun findDetailById(@Param("id") id: UUID): ApplicationEvaluation?
}
