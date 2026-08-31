package com.uade.dda2.server.feature.auth.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.MapsId
import jakarta.persistence.OneToOne
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import org.hibernate.annotations.OnDelete
import org.hibernate.annotations.OnDeleteAction
import java.time.Instant

/** Optional local data copy. Neither the snapshot nor matching personal data proves identity. */
@Entity
@Table(name = "citizen_snapshot")
class CitizenSnapshot(
    @Id
    @Column(name = "user_id")
    var userId: Long? = null,

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    var user: User,

    @Column(name = "full_name", length = 150)
    var fullName: String? = null,

    @Column(length = 50)
    var dni: String? = null,

    @Column(length = 500)
    var address: String? = null,

    @Column(length = 50)
    var phone: String? = null,

    @Column(length = 180)
    var email: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = createdAt,
) {
    @PreUpdate
    fun markUpdated() {
        updatedAt = Instant.now()
    }
}
