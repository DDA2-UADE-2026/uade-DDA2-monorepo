package com.uade.dda2.server.feature.integration.entity

import jakarta.persistence.*
import com.uade.dda2.server.feature.application.entity.ApplicationDocumentStatus
import com.uade.dda2.server.feature.auth.entity.User
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "social_external_documentation", indexes = [Index(name = "ix_social_external_docs_application", columnList = "application_id")])
class ExternalDocumentationReceipt(
    @Id var requestId: UUID,
    @Column(name = "application_id", nullable = false) var applicationId: UUID,
    @Column(name = "requirement_id", nullable = false) var requirementId: UUID,
    @Column(name = "file_id", nullable = false) var fileId: UUID,
    @Column(name = "file_number", nullable = false, length = 120) var fileNumber: String,
    @Column(name = "document_id") var documentId: UUID? = null,
    @Column(nullable = false, length = 30) var status: String,
    @Column(name = "last_event_id", nullable = false) var lastEventId: UUID,
    @Column(name = "source_updated_at", nullable = false) var sourceUpdatedAt: Instant,
    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 20) var reviewStatus: ApplicationDocumentStatus = ApplicationDocumentStatus.PENDING,
    @Column(length = 1000) var observation: String? = null,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "reviewed_by_user_id") var reviewedBy: User? = null,
    @Column(name = "reviewed_at") var reviewedAt: Instant? = null,
)
