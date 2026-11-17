package com.uade.dda2.server.feature.auth

import com.sun.net.httpserver.HttpServer
import com.uade.dda2.server.feature.auth.dto.M1LocalAccount
import com.uade.dda2.server.feature.auth.dto.request.M1SessionLoginRequest
import com.uade.dda2.server.feature.auth.entity.Permission
import com.uade.dda2.server.feature.auth.entity.Role
import com.uade.dda2.server.feature.auth.entity.User
import com.uade.dda2.server.feature.auth.repository.PermissionRepository
import com.uade.dda2.server.feature.auth.repository.RoleRepository
import com.uade.dda2.server.feature.auth.repository.UserRepository
import com.uade.dda2.server.feature.auth.service.AuthService
import com.uade.dda2.server.feature.integration.error.M1AuthException
import com.uade.dda2.server.feature.integration.error.M1AuthFailure
import com.uade.dda2.server.feature.log.entity.LogAction
import com.uade.dda2.server.feature.log.repository.LogRepository
import com.uade.dda2.server.security.ACCESS_COOKIE
import com.uade.dda2.server.security.CSRF_COOKIE
import com.uade.dda2.server.security.JwtAuthSource
import com.uade.dda2.server.security.JwtService
import com.uade.dda2.server.security.ROLE_SELECTION_COOKIE
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.json.JsonMapper
import java.net.InetSocketAddress
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.*

@SpringBootTest(properties = [
    "spring.datasource.url=jdbc:h2:mem:m1-session;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=VALUE",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true",
    "app.enrollment-period.expiration.cron=-", "app.security.cookie.secure=true", "app.security.cookie.same-site=Lax",
    "security.rate-limit.auth.max-attempts=3",
])
@AutoConfigureMockMvc
@ActiveProfiles("docs")
class M1SessionFlowTest {
    @Autowired lateinit var mvc: MockMvc
    @Autowired lateinit var json: JsonMapper
    @Autowired lateinit var jwt: JwtService
    @Autowired lateinit var encoder: PasswordEncoder
    @Autowired lateinit var users: UserRepository
    @Autowired lateinit var roles: RoleRepository
    @Autowired lateinit var permissions: PermissionRepository
    @Autowired lateinit var audit: LogRepository
    @Autowired lateinit var auth: AuthService
    @Autowired lateinit var manager: PlatformTransactionManager
    private lateinit var address: String
    private val citizenId = UUID.fromString("b63f5a2e-4b71-46e7-8b01-381c85d88f62")
    private val credentials = """{"email":"ciudadano@example.invalid","password":"  clave con espacios  "}"""

    data class Reply(val status: Int = 200, val body: String = success, val delay: Long = 0)

    companion object {
        private val success = Files.readString(Path.of("docs/external/m1-auth/login-success.json"))
        private val reply = AtomicReference(Reply())
        private val calls = AtomicInteger()
        private val requestBody = AtomicReference<String>()
        private val ipSequence = AtomicInteger()
        private val executor = Executors.newCachedThreadPool()
        private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
            this.executor = M1SessionFlowTest.executor
            createContext("/auth/usuario/login") { exchange ->
                calls.incrementAndGet()
                requestBody.set(exchange.requestBody.bufferedReader(Charsets.UTF_8).use { it.readText() })
                val response = reply.get()
                if (response.delay > 0) Thread.sleep(response.delay)
                runCatching {
                    exchange.responseHeaders.add("Content-Type", "application/json")
                    exchange.responseHeaders.add("Authorization", "Bearer m1-private-test-access")
                    exchange.responseHeaders.add("X-Refresh-Token", "m1-private-test-refresh")
                    val bytes = response.body.toByteArray(Charsets.UTF_8)
                    exchange.sendResponseHeaders(response.status, bytes.size.toLong())
                    exchange.responseBody.use { it.write(bytes) }
                }
                exchange.close()
            }
            start()
        }

        @JvmStatic @DynamicPropertySource fun remote(registry: DynamicPropertyRegistry) {
            registry.add("app.m1.auth.enabled") { true }
            registry.add("app.m1.auth.base-url") { "http://127.0.0.1:${server.address.port}" }
            registry.add("app.m1.auth.read-timeout") { "500ms" }
        }

        @JvmStatic @AfterAll fun stop() { server.stop(0); executor.shutdownNow() }
    }

    private fun <T> tx(action: () -> T): T = requireNotNull(TransactionTemplate(manager).execute { action() })

    @BeforeEach fun setup() {
        address = "10.40.0.${ipSequence.incrementAndGet()}"
        calls.set(0)
        requestBody.set("")
        reply.set(Reply())
        tx {
            audit.deleteAll()
            users.deleteAll()
            roles.deleteAll()
            permissions.deleteAll()
            permissions.flush()
            val own = permissions.save(Permission(name = "applications:own:view"))
            val admin = permissions.save(Permission(name = "users:view"))
            roles.save(Role(name = "CIUDADANO", permissions = mutableSetOf(own)))
            roles.save(Role(name = "ADMIN", permissions = mutableSetOf(admin)))
        }
    }

    private data class Csrf(val cookie: Cookie, val token: String)
    private fun csrf(vararg cookies: Cookie): Csrf {
        val request = get("/auth/session/csrf")
        if (cookies.isNotEmpty()) request.cookie(*cookies)
        val result = mvc.perform(request).andExpect(status().isOk).andReturn().response
        return Csrf(assertNotNull(result.getCookie(CSRF_COOKIE)), json.readTree(result.contentAsString)["token"].asString())
    }

    private fun login(body: String = credentials, vararg cookies: Cookie): ResultActions {
        val csrf = csrf(*cookies)
        return mvc.perform(post("/auth/session/m1/login").cookie(csrf.cookie, *cookies).header("X-XSRF-TOKEN", csrf.token)
            .with { it.remoteAddr = address; it }.contentType(MediaType.APPLICATION_JSON).content(body))
    }

    private fun staff(active: Boolean = true, citizen: Boolean = true): Long = tx {
        val assigned = roles.findByNameIn(if (citizen) listOf("CIUDADANO", "ADMIN") else listOf("ADMIN"))
        requireNotNull(users.saveAndFlush(User(username = "staff", passwordHash = encoder.encode("local-password"),
            externalCitizenId = citizenId.toString(), name = "Personal local", email = "staff@example.invalid",
            active = active, roles = assigned.toMutableSet())).id)
    }

    private fun access(result: MvcResult): Cookie = assertNotNull(result.response.getCookie(ACCESS_COOKIE))

    @Test fun `proxy completo emite cookie M8 y audita alta e ingreso sin exponer tokens M1`() {
        val result = login().andExpect(status().isOk)
            .andExpect(jsonPath("$.requiresRoleSelection").value(false))
            .andExpect(jsonPath("$.user.roles[0]").value("CIUDADANO"))
            .andExpect(jsonPath("$.user.activeRole").value("CIUDADANO"))
            .andExpect(jsonPath("$.user.permissions[0]").value("applications:own:view"))
            .andExpect(jsonPath("$.token").doesNotExist()).andExpect(jsonPath("$.selectionToken").doesNotExist())
            .andExpect(cookie().httpOnly(ACCESS_COOKIE, true)).andExpect(cookie().secure(ACCESS_COOKIE, true))
            .andExpect(cookie().path(ACCESS_COOKIE, "/")).andExpect(cookie().maxAge(ROLE_SELECTION_COOKIE, 0))
            .andExpect(cookie().maxAge(CSRF_COOKIE, 0)).andExpect(header().doesNotExist("Authorization"))
            .andExpect(header().doesNotExist("X-Refresh-Token")).andReturn()
        val session = access(result)
        val principal = jwt.parse(session.value)
        assertEquals(JwtAuthSource.M1, principal.authSource)
        assertEquals("CIUDADANO", principal.activeRole)
        assertTrue(session.maxAge > 0)
        assertTrue(result.response.getHeaders("Set-Cookie").any { it.startsWith("$ACCESS_COOKIE=") && it.contains("SameSite=Lax") && !it.contains("Domain=") })
        assertFalse(result.response.contentAsString.contains("m1-private-test"))
        val sent = json.readTree(requestBody.get())
        assertEquals(setOf("gmail", "password"), sent.properties().map { it.key }.toSet())
        assertEquals("ciudadano@example.invalid", sent["gmail"].asString())
        assertEquals("  clave con espacios  ", sent["password"].asString())
        assertEquals(1, calls.get())
        tx {
            val user = users.findById(principal.id).orElseThrow()
            assertNull(user.username)
            assertNull(user.passwordHash)
            assertEquals(citizenId.toString(), user.externalCitizenId)
            assertEquals(listOf(LogAction.CREATE, LogAction.LOGIN), audit.findAll().map { it.action }.sortedBy { it.ordinal })
        }
        mvc.perform(get("/auth/me").cookie(session)).andExpect(status().isOk)
            .andExpect(jsonPath("$.user.roles.length()").value(1))
        mvc.perform(get("/users").cookie(session)).andExpect(status().isForbidden)
    }

    @Test fun `cuenta con roles administrativos solo ve ciudadano y no puede elevarse por cookies ni Bearer`() {
        val id = staff()
        val result = login().andExpect(status().isOk).andExpect(jsonPath("$.user.id").value(id))
            .andExpect(jsonPath("$.user.roles.length()").value(1)).andReturn()
        val session = access(result)
        val csrf = csrf(session)
        mvc.perform(post("/auth/session/switch-role").cookie(session, csrf.cookie).header("X-XSRF-TOKEN", csrf.token)
            .contentType(MediaType.APPLICATION_JSON).content("""{"role":"ADMIN"}"""))
            .andExpect(status().isForbidden).andExpect(jsonPath("$.code").value("AUTH_ROLE_NOT_ALLOWED_FOR_SOURCE"))
        mvc.perform(post("/auth/switch-role").header("Authorization", "Bearer ${session.value}")
            .contentType(MediaType.APPLICATION_JSON).content("""{"role":"ADMIN"}"""))
            .andExpect(status().isForbidden)
        mvc.perform(get("/auth/me").cookie(session)).andExpect(status().isOk)
            .andExpect(jsonPath("$.user.roles.length()").value(1)).andExpect(jsonPath("$.user.name").value("Personal local"))
        tx { assertEquals(setOf("CIUDADANO", "ADMIN"), users.findByIdWithRoles(id)!!.roles.map { it.name }.toSet()) }
    }

    @Test fun `cambiar a ciudadano conserva el origen M1 e impide un segundo cambio hacia ADMIN`() {
        staff()
        val session = access(login().andExpect(status().isOk).andReturn())
        val csrf = csrf(session)
        val changed = mvc.perform(post("/auth/session/switch-role").cookie(session, csrf.cookie).header("X-XSRF-TOKEN", csrf.token)
            .contentType(MediaType.APPLICATION_JSON).content("""{"role":"CIUDADANO"}"""))
            .andExpect(status().isOk).andReturn()
        val next = access(changed)
        assertEquals(JwtAuthSource.M1, jwt.parse(next.value).authSource)
        mvc.perform(post("/auth/switch-role").header("Authorization", "Bearer ${next.value}")
            .contentType(MediaType.APPLICATION_JSON).content("""{"role":"ADMIN"}"""))
            .andExpect(status().isForbidden)
    }

    @Test fun `token M1 no habilita seleccion de rol y reemplaza la cookie de seleccion anterior`() {
        val result = login(credentials, Cookie(ROLE_SELECTION_COOKIE, "old-selection"))
            .andExpect(status().isOk).andExpect(cookie().maxAge(ROLE_SELECTION_COOKIE, 0)).andReturn()
        val session = access(result)
        mvc.perform(get("/auth/session/role-selection").cookie(session)).andExpect(status().isUnauthorized)
        mvc.perform(post("/auth/select-role").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(mapOf("selectionToken" to session.value, "role" to "ADMIN"))))
            .andExpect(status().isUnauthorized)
    }

    @Test fun `sin CSRF no llama a M1 aunque se envie Authorization`() {
        for (authorization in listOf("", "Bearer a-token")) {
            mvc.perform(post("/auth/session/m1/login").header("Authorization", authorization)
                .contentType(MediaType.APPLICATION_JSON).content(credentials))
                .andExpect(status().isForbidden).andExpect(jsonPath("$.code").value("AUTH_CSRF_INVALID"))
        }
        assertEquals(0, calls.get())
        tx { assertEquals(0, users.count()) }
    }

    @Test fun `una cookie anterior invalida no bloquea el ingreso y logout borra la sesion`() {
        val result = login(credentials, Cookie(ACCESS_COOKIE, "expired-or-invalid")).andExpect(status().isOk).andReturn()
        val session = access(result)
        val csrf = csrf(session)
        mvc.perform(post("/auth/session/logout").cookie(session, csrf.cookie).header("X-XSRF-TOKEN", csrf.token))
            .andExpect(status().isNoContent).andExpect(cookie().maxAge(ACCESS_COOKIE, 0))
        mvc.perform(get("/auth/me")).andExpect(status().isUnauthorized)
    }

    @ParameterizedTest
    @ValueSource(strings = ["{}", "{\"email\":\"bad\",\"password\":\"secret\"}",
        "{\"email\":\"ciudadano@example.invalid\",\"password\":\" \"}",
        "{\"email\":\"ciudadano@example.invalid\",\"password\":\"secret\",\"citizenId\":\"chosen\"}",
        "{\"email\":\"ciudadano@example.invalid\",\"password\":\"secret\",\"auth_source\":\"LOCAL\"}"])
    fun `validacion rechaza credenciales incompletas y campos de identidad elegidos por el cliente`(body: String) {
        login(body).andExpect(status().isBadRequest).andExpect(cookie().doesNotExist(ACCESS_COOKIE))
        assertEquals(0, calls.get())
        tx { assertEquals(0, users.count()); assertEquals(0, audit.count()) }
    }

    @ParameterizedTest
    @CsvSource("401,401,M1_AUTH_INVALID_CREDENTIALS", "403,403,M1_AUTH_ACCOUNT_FORBIDDEN", "429,429,M1_AUTH_RATE_LIMITED",
        "500,502,M1_AUTH_UPSTREAM_ERROR", "503,503,M1_AUTH_UNAVAILABLE")
    fun `errores M1 mantienen contrato local sin cookies ni cuerpos remotos`(remote: Int, local: Int, code: String) {
        reply.set(Reply(remote, "private upstream body"))
        val result = login().andExpect(status().`is`(local)).andExpect(jsonPath("$.code").value(code))
            .andExpect(cookie().doesNotExist(ACCESS_COOKIE)).andReturn()
        assertEquals(1, calls.get())
        assertFalse(result.response.contentAsString.contains("private upstream body"))
        tx { assertEquals(0, users.count()); assertEquals(0, audit.count()) }
    }

    @Test fun `timeout no emite sesion ni aprovisiona usuario`() {
        reply.set(Reply(delay = 1200))
        login().andExpect(status().isGatewayTimeout).andExpect(jsonPath("$.code").value("M1_AUTH_TIMEOUT"))
            .andExpect(cookie().doesNotExist(ACCESS_COOKIE))
        assertEquals(1, calls.get())
        tx { assertEquals(0, users.count()); assertEquals(0, audit.count()) }
    }

    @Test fun `respuesta exitosa invalida o actor empleado no llegan al aprovisionamiento`() {
        reply.set(Reply(body = "{}"))
        login().andExpect(status().isBadGateway).andExpect(cookie().doesNotExist(ACCESS_COOKIE))
        reply.set(Reply(body = success.replace("\"actorType\": \"CIUDADANO\"", "\"actorType\": \"EMPLEADO\"")))
        login().andExpect(status().isForbidden).andExpect(jsonPath("$.code").value("M1_AUTH_ACTOR_NOT_ALLOWED"))
        tx { assertEquals(0, users.count()) }
    }

    @ParameterizedTest @ValueSource(strings = ["inactive", "no-role"])
    fun `cuenta local sin acceso no recibe sesion despues de un login M1 valido`(scenario: String) {
        staff(active = scenario != "inactive", citizen = scenario != "no-role")
        val code = if (scenario == "inactive") "M1_AUTH_LOCAL_ACCOUNT_INACTIVE" else "M1_AUTH_CITIZEN_ROLE_REQUIRED"
        login().andExpect(status().isForbidden).andExpect(jsonPath("$.code").value(code))
            .andExpect(cookie().doesNotExist(ACCESS_COOKIE))
        tx { assertEquals(1, users.count()); assertEquals(0, audit.count()) }
    }

    @Test fun `el limite local evita llamadas adicionales al login remoto`() {
        reply.set(Reply(401, "{}"))
        repeat(3) { login().andExpect(status().isUnauthorized) }
        login().andExpect(status().isTooManyRequests).andExpect(jsonPath("$.code").value("AUTH_RATE_LIMITED"))
            .andExpect(header().exists("Retry-After"))
        assertEquals(3, calls.get())
    }

    @ParameterizedTest @ValueSource(strings = ["inactive", "no-role", "other-link", "missing"])
    fun `emision vuelve a comprobar estado rol y vinculo despues de resolver la cuenta`(scenario: String) {
        val id = staff()
        tx {
            val user = users.findById(id).orElseThrow()
            when (scenario) {
                "inactive" -> user.active = false
                "no-role" -> user.roles.removeIf { it.name == "CIUDADANO" }
                "other-link" -> user.externalCitizenId = UUID.randomUUID().toString()
            }
        }
        val account = M1LocalAccount(if (scenario == "missing") Long.MAX_VALUE else id, citizenId, false)
        val expected = when (scenario) {
            "inactive" -> M1AuthFailure.LOCAL_ACCOUNT_INACTIVE
            "no-role" -> M1AuthFailure.CITIZEN_ROLE_REQUIRED
            else -> M1AuthFailure.IDENTITY_CONFLICT
        }
        assertEquals(expected, assertFailsWith<M1AuthException> { auth.startM1Session(account) }.failure)
        tx { assertEquals(0, audit.count()) }
    }

    @Test fun `DTO no imprime correo ni password en su representacion`() {
        val request = M1SessionLoginRequest("citizen@example.invalid", "private-password")
        assertFalse(request.toString().contains(request.email))
        assertFalse(request.toString().contains(request.password))
    }
}
