package com.uade.dda2.server.feature.application.repository

import com.uade.dda2.server.feature.application.entity.ApplicationDocument
import com.uade.dda2.server.feature.application.entity.ApplicationDocumentStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime
import java.util.UUID

interface ApplicationDocumentMetadataProjection {
    val id: UUID
    val applicationId: UUID
    val requirementId: UUID
    val requirementCode: String
    val requirementName: String
    val required: Boolean
    val documentId: UUID
    val originalName: String
    val contentType: String
    val sizeBytes: Long
    val status: ApplicationDocumentStatus
    val observation: String?
    val uploadedAt: LocalDateTime
    val reviewedByUserId: Long?
    val reviewedAt: LocalDateTime?
}

interface ApplicationDocumentStateProjection {
    val applicationId: UUID
    val requirementId: UUID
    val status: ApplicationDocumentStatus
    val observation: String?
}

interface ApplicationDocumentContentProjection {
    val applicationDocumentId: UUID
    val originalName: String
    val contentType: String
    val sizeBytes: Long
    val content: ByteArray
}

interface ApplicationDocumentRepository : JpaRepository<ApplicationDocument, UUID> {
    @Query("""
        select ad.id as id, a.id as applicationId,
               r.id as requirementId, r.code as requirementCode, r.name as requirementName, r.required as required,
               d.id as documentId, d.originalName as originalName, d.contentType as contentType, d.sizeBytes as sizeBytes,
               ad.status as status, ad.observation as observation, ad.uploadedAt as uploadedAt,
               reviewer.id as reviewedByUserId, ad.reviewedAt as reviewedAt
        from ApplicationDocument ad
        join ad.application a join ad.requirement r join ad.document d
        left join ad.reviewedBy reviewer
        where a.id = :applicationId
        order by r.name, r.code
    """)
    fun findMetadataByApplicationId(@Param("applicationId") applicationId: UUID): List<ApplicationDocumentMetadataProjection>

    @Query("""
        select ad.id as id, a.id as applicationId,
               r.id as requirementId, r.code as requirementCode, r.name as requirementName, r.required as required,
               d.id as documentId, d.originalName as originalName, d.contentType as contentType, d.sizeBytes as sizeBytes,
               ad.status as status, ad.observation as observation, ad.uploadedAt as uploadedAt,
               reviewer.id as reviewedByUserId, ad.reviewedAt as reviewedAt
        from ApplicationDocument ad
        join ad.application a join ad.requirement r join ad.document d
        left join ad.reviewedBy reviewer
        where ad.id = :id and a.id = :applicationId
    """)
    fun findMetadataByIdAndApplicationId(
        @Param("id") id: UUID,
        @Param("applicationId") applicationId: UUID,
    ): ApplicationDocumentMetadataProjection?

    @Query("select ad from ApplicationDocument ad join fetch ad.document join fetch ad.requirement where ad.application.id = :applicationId and ad.requirement.id = :requirementId")
    fun findEntityByApplicationIdAndRequirementId(
        @Param("applicationId") applicationId: UUID,
        @Param("requirementId") requirementId: UUID,
    ): ApplicationDocument?

    @Query("select ad from ApplicationDocument ad join fetch ad.document join fetch ad.requirement where ad.id = :id and ad.application.id = :applicationId")
    fun findEntityByIdAndApplicationId(
        @Param("id") id: UUID,
        @Param("applicationId") applicationId: UUID,
    ): ApplicationDocument?

    @Query("""
        select ad.id as applicationDocumentId, d.originalName as originalName, d.contentType as contentType,
               d.sizeBytes as sizeBytes, d.content as content
        from ApplicationDocument ad join ad.document d
        where ad.id = :id and ad.application.id = :applicationId
    """)
    fun findContentByIdAndApplicationId(
        @Param("id") id: UUID,
        @Param("applicationId") applicationId: UUID,
    ): ApplicationDocumentContentProjection?

    @Query("select ad.application.id as applicationId, ad.requirement.id as requirementId, ad.status as status, ad.observation as observation from ApplicationDocument ad where ad.application.id in :applicationIds")
    fun findStatesByApplicationIdIn(@Param("applicationIds") applicationIds: Collection<UUID>): List<ApplicationDocumentStateProjection>

    fun existsByReviewedById(userId: Long): Boolean
}
