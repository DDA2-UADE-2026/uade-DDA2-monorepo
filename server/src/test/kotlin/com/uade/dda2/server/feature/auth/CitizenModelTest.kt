package com.uade.dda2.server.feature.auth

import com.uade.dda2.server.feature.auth.entity.CitizenSnapshot
import com.uade.dda2.server.feature.auth.entity.User
import com.uade.dda2.server.feature.auth.repository.CitizenSnapshotRepository
import com.uade.dda2.server.feature.auth.repository.UserRepository
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.test.context.ActiveProfiles
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@DataJpaTest(properties = [
    "spring.test.database.replace=NONE",
    "spring.datasource.url=jdbc:h2:mem:citizen-model;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=VALUE",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true",
])
@ActiveProfiles("docs")
class CitizenModelTest {
    @Autowired lateinit var users: UserRepository
    @Autowired lateinit var snapshots: CitizenSnapshotRepository
    @Autowired lateinit var entityManager: EntityManager

    @Test
    fun `usuarios sin credenciales locales ni snapshot tienen identidad propia`() {
        val first = users.saveAndFlush(User(name = "First", email = "first@example.com"))
        val second = users.saveAndFlush(User(name = "Second", email = "second@example.com"))
        assertTrue(first.id != second.id)
        assertNull(first.username)
        assertNull(first.passwordHash)
        assertFalse(snapshots.existsById(first.id!!))
    }

    @Test
    fun `vinculo externo es unico e independiente de snapshot`() {
        val user = users.saveAndFlush(User(name = "Linked", email = "linked@example.com", externalCitizenId = "citizen-1"))
        assertEquals(user.id, users.findByExternalCitizenId("citizen-1")?.id)
        assertFalse(snapshots.existsById(user.id!!))
        assertFailsWith<DataIntegrityViolationException> {
            users.saveAndFlush(User(name = "Other", email = "other@example.com", externalCitizenId = "citizen-1"))
        }
    }

    @Test
    fun `snapshot usa la PK del usuario y se elimina con el usuario`() {
        val user = users.saveAndFlush(User(name = "User", email = "user@example.com"))
        val id = user.id!!
        snapshots.saveAndFlush(CitizenSnapshot(user = user, fullName = "Nombre Completo", dni = "00123456",
            address = "Calle 123", phone = "+54 11 1234 5678", email = "snapshot@example.com"))
        entityManager.clear()
        val snapshot = snapshots.findById(id).orElseThrow()
        assertEquals(id, snapshot.userId)
        assertEquals("00123456", snapshot.dni)
        assertEquals("user@example.com", snapshot.user.email)
        assertEquals("snapshot@example.com", snapshot.email)
        users.deleteById(id)
        users.flush()
        entityManager.clear()
        assertFalse(snapshots.existsById(id))
    }

    @Test
    fun `base rechaza credenciales locales incompletas`() {
        assertFailsWith<DataIntegrityViolationException> {
            users.saveAndFlush(User(username = "incomplete", name = "User", email = "user@example.com"))
        }
    }
}
