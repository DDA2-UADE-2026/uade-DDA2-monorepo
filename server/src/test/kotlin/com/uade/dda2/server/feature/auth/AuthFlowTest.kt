package com.uade.dda2.server.feature.auth

import com.uade.dda2.server.feature.auth.dto.response.LoginResponse
import com.uade.dda2.server.feature.auth.entity.Permission
import com.uade.dda2.server.feature.auth.entity.Role
import com.uade.dda2.server.feature.auth.entity.User
import com.uade.dda2.server.feature.auth.repository.RoleRepository
import com.uade.dda2.server.feature.auth.repository.UserRepository
import com.uade.dda2.server.feature.log.service.LogService
import com.uade.dda2.server.security.JwtService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.json.JsonMapper
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("docs")
class AuthFlowTest {
    @Autowired lateinit var mvc: MockMvc
    @Autowired lateinit var json: JsonMapper
    @Autowired lateinit var jwt: JwtService
    @Autowired lateinit var encoder: PasswordEncoder
    @MockitoBean lateinit var users: UserRepository
    @MockitoBean lateinit var roles: RoleRepository
    @MockitoBean lateinit var logs: LogService

    private lateinit var user: User
    private lateinit var citizen: Role
    private lateinit var auditor: Role

    @BeforeEach
    fun setup() {
        citizen = Role(1, "CIUDADANO")
        auditor = Role(2, "AUDITOR", mutableSetOf(Permission(1, "roles:view")))
        user = User(id = 42, username = "test", passwordHash = encoder.encode("password123"),
            name = "Test", email = "test@example.com", roles = mutableSetOf(citizen, auditor))
        `when`(users.findByUsernameIgnoreCase("test")).thenReturn(user)
        `when`(users.findByIdWithRoles(42)).thenReturn(user)
        `when`(users.getReferenceById(42)).thenReturn(user)
        `when`(roles.findAllByOrderByNameAsc()).thenReturn(listOf(citizen, auditor))
    }

    @Test
    fun `login multirrol no concede acceso antes de seleccionar`() {
        val response = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("""{"username":"test","password":"password123"}"""))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.requiresRoleSelection").value(true))
            .andExpect(jsonPath("$.token").isEmpty)
            .andExpect(jsonPath("$.permissions").doesNotExist())
            .andExpect(jsonPath("$.user.permissions").isEmpty)
            .andExpect(jsonPath("$.user.activeRole").isEmpty)
            .andReturn().response.contentAsString
        val selection = assertNotNull(json.readValue(response, LoginResponse::class.java).selectionToken)
        for (path in listOf("/auth/me", "/api/programs", "/roles", "/actuator/info")) {
            mvc.perform(get(path).header("Authorization", "Bearer $selection"))
                .andExpect(status().isUnauthorized)
        }
        mvc.perform(post("/auth/switch-role").header("Authorization", "Bearer $selection")
            .contentType(MediaType.APPLICATION_JSON).content("""{"role":"AUDITOR"}"""))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `un solo rol se activa automaticamente aunque no tenga permisos`() {
        user.roles.remove(auditor)
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("""{"username":"test","password":"password123"}"""))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.requiresRoleSelection").value(false))
            .andExpect(jsonPath("$.token").isNotEmpty)
            .andExpect(jsonPath("$.user.activeRole").value("CIUDADANO"))
            .andExpect(jsonPath("$.permissions").doesNotExist())
            .andExpect(jsonPath("$.user.permissions").isEmpty)
    }

    @Test
    fun `seleccionar ciudadano no hereda los permisos de auditor`() {
        val token = selected("CIUDADANO")
        mvc.perform(get("/auth/me").header("Authorization", "bearer $token"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.user.activeRole").value("CIUDADANO"))
            .andExpect(jsonPath("$.user.permissions").isEmpty)
        mvc.perform(get("/roles").header("Authorization", "Bearer $token"))
            .andExpect(status().isForbidden)
        mvc.perform(get("/roles").header("Authorization", "Bearer ${selected("AUDITOR")}"))
            .andExpect(status().isOk)
    }

    @Test
    fun `seleccion verifica nuevamente roles y estado del usuario`() {
        val selection = jwt.createRoleSelectionToken(user)
        user.roles.remove(auditor)
        select(selection, "AUDITOR").andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("AUTH_ROLE_NOT_ASSIGNED"))
        user.active = false
        select(selection, "CIUDADANO").andExpect(status().isUnauthorized)
    }

    @Test
    fun `rechaza credenciales operativas o falsas como token de seleccion`() {
        select(jwt.createToken(user, auditor), "AUDITOR").andExpect(status().isUnauthorized)
        select("not-a-jwt", "AUDITOR").andExpect(status().isUnauthorized)
        select(jwt.createRoleSelectionToken(user), "ADMIN").andExpect(status().isForbidden)
    }

    @Test
    fun `cambiar rol emite permisos actuales sin invalidar el JWT anterior`() {
        val previous = jwt.createToken(user, auditor)
        auditor.permissions.add(Permission(2, "users:view"))
        mvc.perform(get("/auth/me").header("Authorization", "Bearer $previous"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.user.permissions.length()").value(1))
        val body = mvc.perform(post("/auth/switch-role").header("Authorization", "Bearer $previous")
            .contentType(MediaType.APPLICATION_JSON).content("""{"role":"CIUDADANO"}"""))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.user.activeRole").value("CIUDADANO"))
            .andExpect(jsonPath("$.permissions").doesNotExist())
            .andExpect(jsonPath("$.user.permissions").isEmpty)
            .andReturn().response.contentAsString
        val current = assertNotNull(json.readValue(body, LoginResponse::class.java).token)
        assertEquals("CIUDADANO", jwt.parse(current).activeRole)
        assertEquals(listOf("roles:view"), jwt.parse(previous).permissions)
        mvc.perform(get("/roles").header("Authorization", "Bearer $previous"))
            .andExpect(status().isOk)
        mvc.perform(post("/auth/switch-role").header("Authorization", "Bearer $current")
            .contentType(MediaType.APPLICATION_JSON).content("""{"role":"AUDITOR"}"""))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.user.permissions.length()").value(2))
    }

    @Test
    fun `no crea contexto operativo para usuarios sin roles o credenciales locales`() {
        user.roles.clear()
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("""{"username":"test","password":"password123"}"""))
            .andExpect(status().isForbidden).andExpect(jsonPath("$.code").value("AUTH_NO_ROLES"))
        user.passwordHash = null
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("""{"username":"test","password":"password123"}"""))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `switch requiere JWT y rechaza un rol no asignado`() {
        mvc.perform(post("/auth/switch-role").contentType(MediaType.APPLICATION_JSON)
            .content("""{"role":"CIUDADANO"}"""))
            .andExpect(status().isUnauthorized)
        mvc.perform(post("/auth/switch-role").header("Authorization", "Bearer ${jwt.createToken(user, citizen)}")
            .contentType(MediaType.APPLICATION_JSON).content("""{"role":"ADMIN"}"""))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `administracion conserva cuentas externas y agrega credenciales al mismo User`() {
        auditor.permissions.add(Permission(3, "users:edit"))
        val token = jwt.createToken(user, auditor)
        val external = User(id = 77, name = "External", email = "external@example.com", externalCitizenId = "citizen-77",
            roles = mutableSetOf(citizen))
        `when`(users.findByIdWithRoles(77)).thenReturn(external)
        `when`(roles.findByNameIn(setOf("CIUDADANO"))).thenReturn(listOf(citizen))
        val fields = mapOf("name" to "External", "email" to "external@example.com", "roles" to listOf("CIUDADANO"))
        mvc.perform(put("/users/77").header("Authorization", "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(fields)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.username").isEmpty)
            .andExpect(jsonPath("$.hasLocalCredentials").value(false))
            .andExpect(jsonPath("$.permissionsByRole.CIUDADANO").isEmpty)
        mvc.perform(put("/users/77").header("Authorization", "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(fields + mapOf("username" to "external-local", "password" to "password123"))))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(77))
            .andExpect(jsonPath("$.hasLocalCredentials").value(true))
            .andExpect(jsonPath("$.externalCitizenId").value("citizen-77"))
        assertEquals("external-local", external.username)
        assertTrue(encoder.matches("password123", external.passwordHash))
    }

    @Test
    fun `administracion no permite agregar solo media credencial local`() {
        auditor.permissions.add(Permission(3, "users:edit"))
        val external = User(id = 77, name = "External", email = "external@example.com")
        `when`(users.findByIdWithRoles(77)).thenReturn(external)
        val fields = mapOf("name" to "External", "email" to "external@example.com")
        for (partial in listOf(mapOf("username" to "external-local"), mapOf("password" to "password123"))) {
            mvc.perform(put("/users/77").header("Authorization", "Bearer ${jwt.createToken(user, auditor)}")
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(fields + partial)))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("USER_INCOMPLETE_LOCAL_CREDENTIALS"))
        }
    }

    private fun select(selection: String, role: String) =
        mvc.perform(post("/auth/select-role").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(mapOf("selectionToken" to selection, "role" to role))))

    private fun selected(role: String): String {
        val body = select(jwt.createRoleSelectionToken(user), role).andExpect(status().isOk)
            .andExpect(jsonPath("$.permissions").doesNotExist())
            .andReturn().response.contentAsString
        return assertNotNull(json.readValue(body, LoginResponse::class.java).token)
    }
}
