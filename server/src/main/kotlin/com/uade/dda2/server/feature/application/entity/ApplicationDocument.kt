package com.uade.dda2.server.feature.application.entity

import com.uade.dda2.server.feature.auth.entity.User
import com.uade.dda2.server.feature.document.entity.Document
import com.uade.dda2.server.feature.program.entity.ProgramDocumentRequirement
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "application_document", uniqueConstraints = [
    UniqueConstraint(name = "uk_application_document_application_requirement", columnNames = ["application_id", "requirement_id"]),
    UniqueConstraint(name = "uk_application_document_document", columnNames = ["document_id"]),
], indexes = [
    Index(name = "ix_application_document_application", columnList = "application_id"),
    Index(name = "ix_application_document_requirement", columnList = "requirement_id"),
    Index(name = "ix_application_document_reviewer", columnList = "reviewed_by_user_id"),
], check = [CheckConstraint(name = "ck_application_document_review", constraint =
    "(status = 'PENDING' and observation is null and reviewed_by_user_id is null and reviewed_at is null) or " +
        "(status = 'VALID' and observation is null and reviewed_by_user_id is not null and reviewed_at is not null) or " +
        "(status = 'OBSERVED' and observation is not null and reviewed_by_user_id is not null and reviewed_at is not null)")])
class ApplicationDocument(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false, updatable = false)
    var application: Application,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requirement_id", nullable = false, updatable = false)
    var requirement: ProgramDocumentRequirement,

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false, unique = true)
    var document: Document,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: ApplicationDocumentStatus = ApplicationDocumentStatus.PENDING,

    @Column(length = 1000)
    var observation: String? = null,

    @Column(name = "uploaded_at", nullable = false)
    var uploadedAt: LocalDateTime,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_user_id")
    var reviewedBy: User? = null,

    @Column(name = "reviewed_at")
    var reviewedAt: LocalDateTime? = null,
)
