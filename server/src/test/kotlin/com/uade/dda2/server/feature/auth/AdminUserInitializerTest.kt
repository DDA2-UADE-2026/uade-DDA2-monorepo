package com.uade.dda2.server.feature.auth

import com.uade.dda2.server.config.AdminUserInitializer
import com.uade.dda2.server.feature.auth.entity.Permission
import com.uade.dda2.server.feature.auth.entity.Role
import com.uade.dda2.server.feature.auth.entity.User
import com.uade.dda2.server.feature.auth.dto.response.LoginResponse
import com.uade.dda2.server.feature.auth.repository.PermissionRepository
import com.uade.dda2.server.feature.auth.repository.RoleRepository
import com.uade.dda2.server.feature.auth.repository.UserRepository
import com.uade.dda2.server.security.JwtService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.DefaultApplicationArguments
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.json.JsonMapper
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringBootTest(properties = [
    "app.bootstrap.admin.enabled=true",
    "spring.datasource.url=jdbc:h2:mem:admin-bootstrap;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=VALUE",
    "spring.jpa.hibernate.ddl-auto=create-drop",
])
@ActiveProfiles("docs")
@AutoConfigureMockMvc
@Transactional
class AdminUserInitializerTest {
    @Autowired lateinit var initializer: AdminUserInitializer
    @Autowired lateinit var users: UserRepository
    @Autowired lateinit var roles: RoleRepository
    @Autowired lateinit var permissions: PermissionRepository
    @Autowired lateinit var encoder: PasswordEncoder
    @Autowired lateinit var mvc: MockMvc
    @Autowired lateinit var json: JsonMapper
    @Autowired lateinit var jwt: JwtService

    @Test
    fun `startup creates an active admin with an encoded password and repeated runs do nothing`() {
        val admin = assertNotNull(users.findByUsernameIgnoreCase("admin"))
        assertTrue(admin.active)
        assertTrue(encoder.matches("admin123", admin.passwordHash))
        assertNotEquals("admin123", admin.passwordHash)
        assertEquals(setOf("ADMIN", "CIUDADANO"), admin.roles.map { it.name }.toSet())
        assertTrue(admin.roles.first { it.name == "CIUDADANO" }.permissions.any { it.name == "activities:own:view" })
        assertTrue(admin.roles.first { it.name == "ADMIN" }.permissions.any { it.name == "users:view" })
        val originalHash = admin.passwordHash

        initializer.run(DefaultApplicationArguments())

        assertEquals(1L, users.count())
        assertEquals(2L, roles.count())
        assertEquals(originalHash, users.findByUsernameIgnoreCase("admin")!!.passwordHash)
    }

    @Test
    fun `existing mixed case username is never reset reactivated or assigned other roles`() {
        users.deleteAll()
        users.flush()
        val originalHash = encoder.encode("changed-password")
        val existing = users.saveAndFlush(User(
            username = "AdMiN", passwordHash = originalHash, active = false,
            name = "Existing account", email = "existing@example.com",
        ))

        initializer.run(DefaultApplicationArguments())

        val admin = assertNotNull(users.findByUsernameIgnoreCase("admin"))
        assertEquals(existing.id, admin.id)
        assertEquals(originalHash, admin.passwordHash)
        assertEquals(false, admin.active)
        assertEquals("Existing account", admin.name)
        assertEquals("existing@example.com", admin.email)
        assertTrue(admin.roles.isEmpty())
        assertEquals(1L, users.count())
    }

    @Test
    fun `new admin reuses the existing role and preserves its custom permissions`() {
        users.deleteAll()
        users.flush()
        val role = roles.findByNameIn(listOf("ADMIN")).single()
        val permission = permissions.saveAndFlush(Permission(name = "custom:rule"))
        role.permissions.add(permission)
        roles.saveAndFlush(role)

        initializer.run(DefaultApplicationArguments())

        val adminRole = users.findByUsernameIgnoreCase("admin")!!.roles.first { it.name == "ADMIN" }
        assertEquals(role.id, adminRole.id)
        assertTrue(adminRole.permissions.any { it.name == "custom:rule" })
        assertEquals(2L, roles.count())
    }

    @Test
    fun `missing ADMIN role is created with the registered permissions`() {
        users.deleteAll()
        users.flush()
        roles.deleteAll()
        roles.flush()
        initializer.run(DefaultApplicationArguments())

        val adminRole = users.findByUsernameIgnoreCase("admin")!!.roles.first { it.name == "ADMIN" }
        assertEquals("ADMIN", adminRole.name)
        assertTrue(adminRole.permissions.map { it.name }.containsAll(listOf("users:view", "roles:view")))
        assertTrue(encoder.matches("admin123", users.findByUsernameIgnoreCase("admin")!!.passwordHash))
    }

    @Test
    fun `existing admin gains citizen role without changing password status or other roles`() {
        val admin = users.findByUsernameIgnoreCase("admin")!!
        val originalHash = encoder.encode("new-password")
        val extraRole = roles.saveAndFlush(Role(name = "VIEWER"))
        admin.passwordHash = originalHash
        admin.active = false
        admin.roles.removeIf { it.name == "CIUDADANO" }
        admin.roles.add(extraRole)
        users.flush()

        initializer.run(DefaultApplicationArguments())
        users.flush()

        val updated = users.findByUsernameIgnoreCase("admin")!!
        assertEquals(originalHash, updated.passwordHash)
        assertEquals(false, updated.active)
        assertEquals(setOf("ADMIN", "CIUDADANO", "VIEWER"), updated.roles.map { it.name }.toSet())
        assertEquals(1L, users.count())
    }

    @Test
    fun `admin can authenticate as citizen and switch back without sharing permissions`() {
        val loginJson = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("""{"username":"admin","password":"admin123"}"""))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.requiresRoleSelection").value(true))
            .andExpect(jsonPath("$.user.roles").isArray)
            .andReturn().response.contentAsString
        val selectionToken = assertNotNull(json.readValue(loginJson, LoginResponse::class.java).selectionToken)
        val citizenJson = mvc.perform(post("/auth/select-role").contentType(MediaType.APPLICATION_JSON)
            .content("""{"selectionToken":"$selectionToken","role":"CIUDADANO"}"""))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.user.activeRole").value("CIUDADANO"))
            .andReturn().response.contentAsString
        val citizenToken = assertNotNull(json.readValue(citizenJson, LoginResponse::class.java).token)
        mvc.perform(get("/auth/me").header("Authorization", "Bearer $citizenToken"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.user.permissions").isNotEmpty)
            .andExpect(jsonPath("$.user.roles").isArray)
        val adminJson = mvc.perform(post("/auth/switch-role").header("Authorization", "Bearer $citizenToken")
            .contentType(MediaType.APPLICATION_JSON).content("""{"role":"ADMIN"}"""))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.user.activeRole").value("ADMIN"))
            .andReturn().response.contentAsString
        val adminToken = assertNotNull(json.readValue(adminJson, LoginResponse::class.java).token)
        mvc.perform(get("/auth/me").header("Authorization", "Bearer $adminToken"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.user.activeRole").value("ADMIN"))
    }

    @Test
    fun `new users assigned ADMIN also receive CIUDADANO immediately`() {
        val admin = users.findByUsernameIgnoreCase("admin")!!
        val role = admin.roles.first { it.name == "ADMIN" }
        val token = jwt.createToken(admin, role)
        mvc.perform(post("/users").header("Authorization", "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"username":"second.admin","password":"admin123","name":"Second Admin","email":"second@example.com","roles":["ADMIN"]}"""))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.roles").isArray)
        assertEquals(setOf("ADMIN", "CIUDADANO"), users.findByUsernameIgnoreCase("second.admin")!!.roles.map { it.name }.toSet())
    }
}
