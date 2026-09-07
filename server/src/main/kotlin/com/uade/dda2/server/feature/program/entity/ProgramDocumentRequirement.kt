package com.uade.dda2.server.feature.program.entity

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "program_document_requirement", uniqueConstraints = [
    UniqueConstraint(name = "uk_program_document_requirement_edition_code", columnNames = ["program_edition_id", "code"]),
], indexes = [
    Index(name = "ix_program_document_requirement_edition", columnList = "program_edition_id"),
])
class ProgramDocumentRequirement(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "program_edition_id", nullable = false, updatable = false)
    var programEdition: ProgramEdition,

    @Column(nullable = false, length = 50)
    var code: String,

    @Column(nullable = false, length = 150)
    var name: String,

    @Column(length = 500)
    var description: String? = null,

    @Column(name = "is_required", nullable = false)
    var required: Boolean,
) {
    @PrePersist @PreUpdate
    fun normalize() {
        code = code.trim().uppercase()
        name = name.trim()
        description = description?.trim()?.takeIf(String::isNotEmpty)
    }
}
