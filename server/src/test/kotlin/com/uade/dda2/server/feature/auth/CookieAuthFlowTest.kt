package com.uade.dda2.server.feature.auth

import com.uade.dda2.server.config.JwtProperties
import com.uade.dda2.server.config.CorsProperties
import com.uade.dda2.server.security.SecurityConfig
import com.uade.dda2.server.security.JwtAuthenticationFilter
import com.uade.dda2.server.security.SecurityErrorResponseWriter
import com.uade.dda2.server.feature.auth.entity.Permission
import com.uade.dda2.server.feature.auth.entity.Role
import com.uade.dda2.server.feature.auth.entity.User
import com.uade.dda2.server.feature.auth.repository.RoleRepository
import com.uade.dda2.server.feature.auth.repository.UserRepository
import com.uade.dda2.server.feature.log.service.LogService
import com.uade.dda2.server.security.ACCESS_COOKIE
import com.uade.dda2.server.security.CSRF_COOKIE
import com.uade.dda2.server.security.ROLE_SELECTION_COOKIE
import com.uade.dda2.server.security.JwtService
import jakarta.servlet.http.Cookie
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import java.time.Instant
import java.util.Date
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.web.filter.CorsFilter
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.csrf.CsrfTokenRepository
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import tools.jackson.databind.json.JsonMapper
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringBootTest(properties = ["app.security.cookie.secure=true", "app.security.cookie.same-site=Lax"])
@AutoConfigureMockMvc
@ActiveProfiles("docs")
class CookieAuthFlowTest {
    @Autowired lateinit var mvc: MockMvc
    @Autowired lateinit var json: JsonMapper
    @Autowired lateinit var jwt: JwtService
    @Autowired lateinit var encoder: PasswordEncoder
    @Autowired lateinit var jwtProperties: JwtProperties
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
            name = "Test", email = "test@example.com", roles = mutableSetOf(citizen))
        `when`(users.findByUsernameIgnoreCase("test")).thenReturn(user)
        `when`(users.findByIdWithRoles(42)).thenReturn(user)
        `when`(roles.findAllByOrderByNameAsc()).thenReturn(listOf(citizen, auditor))
    }

    @Test
    fun `login issues HttpOnly cookie without exposing JWT and me survives reload`() {
        val access = loginCookie()
        assertEquals("CIUDADANO", jwt.parse(access.value).activeRole)
        repeat(2) {
            mvc.perform(get("/auth/me").cookie(access))
                .andExpect(status().isOk).andExpect(jsonPath("$.user.activeRole").value("CIUDADANO"))
        }
        mvc.perform(get("/roles").cookie(access)).andExpect(status().isForbidden)
        mvc.perform(get("/auth/me")).andExpect(status().isUnauthorized)
    }

    @Test
    fun `login requires csrf even without session or with Bearer`() {
        for (bearer in listOf("", "Bearer ${jwt.createToken(user, citizen)}")) {
            mvc.perform(post("/auth/session/login").header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON).content(credentials))
                .andExpect(status().isForbidden).andExpect(jsonPath("$.code").value("AUTH_CSRF_INVALID"))
        }
    }

    @Test
    fun `cookie writes reject missing and wrong csrf and accept matching token`() {
        user.roles.add(auditor)
        val access = Cookie(ACCESS_COOKIE, jwt.createToken(user, citizen))
        mvc.perform(post("/auth/session/switch-role").cookie(access)
            .contentType(MediaType.APPLICATION_JSON).content("""{"role":"AUDITOR"}"""))
            .andExpect(status().isForbidden)
        val csrf = csrf()
        mvc.perform(post("/auth/session/switch-role").cookie(access, csrf.cookie)
            .header("X-XSRF-TOKEN", "wrong")
            .contentType(MediaType.APPLICATION_JSON).content("""{"role":"AUDITOR"}"""))
            .andExpect(status().isForbidden)
        val result = mvc.perform(post("/auth/session/switch-role").cookie(access, csrf.cookie)
            .header("X-XSRF-TOKEN", csrf.token)
            .contentType(MediaType.APPLICATION_JSON).content("""{"role":"AUDITOR"}"""))
            .andExpect(status().isOk).andExpect(jsonPath("$.user.activeRole").value("AUDITOR"))
            .andExpect(jsonPath("$.token").doesNotExist()).andReturn().response
        mvc.perform(get("/roles").cookie(assertNotNull(result.getCookie(ACCESS_COOKIE))))
            .andExpect(status().isOk)
        // The same CSRF protection applies outside the session endpoints.
        mvc.perform(post("/auth/switch-role").cookie(access)
            .contentType(MediaType.APPLICATION_JSON).content("""{"role":"AUDITOR"}"""))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `multi role cookie cannot authenticate until role selected and pending survives reload`() {
        user.roles.add(auditor)
        val selection = loginCookie(ROLE_SELECTION_COOKIE)
        mvc.perform(get("/auth/me").cookie(selection)).andExpect(status().isUnauthorized)
        mvc.perform(get("/auth/me").cookie(Cookie(ACCESS_COOKIE, selection.value)))
            .andExpect(status().isUnauthorized)
        mvc.perform(get("/auth/session/role-selection").cookie(selection))
            .andExpect(status().isOk).andExpect(jsonPath("$.requiresRoleSelection").value(true))
            .andExpect(jsonPath("$.selectionToken").doesNotExist())
            .andExpect(jsonPath("$.user.permissions").isEmpty)
        val csrf = csrf()
        mvc.perform(post("/auth/session/select-role").cookie(selection, csrf.cookie)
            .header("X-XSRF-TOKEN", csrf.token)
            .contentType(MediaType.APPLICATION_JSON).content("""{"role":"ADMIN"}"""))
            .andExpect(status().isForbidden)
        mvc.perform(post("/auth/session/select-role").cookie(selection, csrf.cookie)
            .header("X-XSRF-TOKEN", csrf.token)
            .contentType(MediaType.APPLICATION_JSON).content("""{"role":"CIUDADANO"}"""))
            .andExpect(status().isOk).andExpect(jsonPath("$.user.activeRole").value("CIUDADANO"))
            .andExpect(jsonPath("$.user.permissions").isEmpty)
            .andExpect(cookie().maxAge(ROLE_SELECTION_COOKIE, 0))
            .andExpect(cookie().exists(ACCESS_COOKIE))
    }

    @Test
    fun `missing invalid and operational selection cookies are rejected`() {
        for (token in listOf("", "invalid", jwt.createToken(user, citizen))) {
            mvc.perform(get("/auth/session/role-selection").cookie(Cookie(ROLE_SELECTION_COOKIE, token)))
                .andExpect(status().isUnauthorized)
        }
    }

    @Test
    fun `expired access and selection JWTs cannot authenticate but allow csrf and logout`() {
        fun expired(type: String): String = Jwts.builder()
            .subject("42").issuer(jwtProperties.issuer)
            .issuedAt(Date.from(Instant.now().minusSeconds(120)))
            .expiration(Date.from(Instant.now().minusSeconds(60)))
            .claim("token_type", type).claim("active_role", "CIUDADANO")
            .claim("permissions", emptyList<String>())
            .signWith(Keys.hmacShaKeyFor(jwtProperties.secret.toByteArray()), Jwts.SIG.HS256).compact()
        val access = Cookie(ACCESS_COOKIE, expired("ACCESS"))
        mvc.perform(get("/auth/me").cookie(access)).andExpect(status().isUnauthorized)
        mvc.perform(get("/auth/session/role-selection").cookie(Cookie(ROLE_SELECTION_COOKIE, expired("ROLE_SELECTION"))))
            .andExpect(status().isUnauthorized)
        val csrf = csrf(access)
        mvc.perform(post("/auth/session/logout").cookie(access, csrf.cookie).header("X-XSRF-TOKEN", csrf.token))
            .andExpect(status().isNoContent).andExpect(cookie().maxAge(ACCESS_COOKIE, 0))
    }

    @Test
    fun `logout clears all cookies including broken session and requires csrf`() {
        val access = Cookie(ACCESS_COOKIE, "expired-or-invalid")
        mvc.perform(post("/auth/session/logout").cookie(access)).andExpect(status().isForbidden)
        val csrf = csrf(access)
        mvc.perform(post("/auth/session/logout").cookie(access, csrf.cookie).header("X-XSRF-TOKEN", csrf.token))
            .andExpect(status().isNoContent)
            .andExpect(cookie().maxAge(ACCESS_COOKIE, 0))
            .andExpect(cookie().maxAge(ROLE_SELECTION_COOKIE, 0))
            .andExpect(cookie().maxAge(CSRF_COOKIE, 0))
        mvc.perform(get("/auth/me")).andExpect(status().isUnauthorized)
    }

    @Test
    fun `broken previous cookie does not prevent login and bad credentials do not issue access`() {
        val csrf = csrf(Cookie(ACCESS_COOKIE, "invalid"))
        mvc.perform(post("/auth/session/login").cookie(csrf.cookie, Cookie(ACCESS_COOKIE, "invalid"))
            .header("X-XSRF-TOKEN", csrf.token)
            .contentType(MediaType.APPLICATION_JSON).content(credentials))
            .andExpect(status().isOk).andExpect(cookie().exists(ACCESS_COOKIE))
        mvc.perform(post("/auth/session/login").cookie(csrf.cookie).header("X-XSRF-TOKEN", csrf.token)
            .contentType(MediaType.APPLICATION_JSON).content("""{"username":"test","password":"incorrect"}"""))
            .andExpect(status().isUnauthorized).andExpect(cookie().doesNotExist(ACCESS_COOKIE))
    }

    @Test
    fun `Bearer stays compatible and wins over cookie without falling back on invalid header`() {
        user.roles.add(auditor)
        val access = Cookie(ACCESS_COOKIE, jwt.createToken(user, citizen))
        val bearer = jwt.createToken(user, auditor)
        mvc.perform(get("/auth/me").cookie(access).header("Authorization", "Bearer $bearer"))
            .andExpect(status().isOk).andExpect(jsonPath("$.user.activeRole").value("AUDITOR"))
        for (header in listOf("Bearer invalid", "Bearer ", "Bearer")) {
            mvc.perform(get("/auth/me").cookie(access).header("Authorization", header))
                .andExpect(status().isUnauthorized)
        }
        mvc.perform(post("/auth/switch-role").cookie(Cookie(ACCESS_COOKIE, "invalid"))
            .header("Authorization", "Bearer $bearer")
            .contentType(MediaType.APPLICATION_JSON).content("""{"role":"CIUDADANO"}"""))
            .andExpect(status().isOk).andExpect(jsonPath("$.token").isNotEmpty)
            .andExpect(cookie().doesNotExist(ACCESS_COOKIE))
    }

    @Test
    fun `CORS permits configured credentialed origin and csrf header but rejects foreign origin`() {
        mvc.perform(options("/auth/session/login").header("Origin", "http://localhost:5173")
            .header("Access-Control-Request-Method", "POST")
            .header("Access-Control-Request-Headers", "content-type,x-xsrf-token"))
            .andExpect(status().isOk)
            .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
            .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
        mvc.perform(get("/auth/session/csrf").header("Origin", "https://untrusted.example"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `each deployed API allows its frontend credentials and rejects the other environment`() {
        val dev = "https://app-muni-uade-dev.x.com"
        val prod = "https://app-muni-uade.x.com"
        for ((allowed, rejected) in listOf(dev to prod, prod to dev)) {
            val properties = CorsProperties(listOf(allowed))
            // Construct the configuration directly: invoking @Bean on Spring's
            // proxy would return the already configured localhost singleton.
            val config = SecurityConfig(mock(JwtAuthenticationFilter::class.java),
                mock(SecurityErrorResponseWriter::class.java), properties, mock(CsrfTokenRepository::class.java))
            val filter = CorsFilter(config.corsConfigurationSource(properties))
            fun preflight(origin: String): MockHttpServletResponse {
                val request = MockHttpServletRequest("OPTIONS", "/auth/session/login").apply {
                    addHeader("Origin", origin)
                    addHeader("Access-Control-Request-Method", "POST")
                    addHeader("Access-Control-Request-Headers", "content-type,x-xsrf-token")
                }
                return MockHttpServletResponse().also { filter.doFilter(request, it, MockFilterChain()) }
            }
            val accepted = preflight(allowed)
            assertEquals(200, accepted.status)
            assertEquals(allowed, accepted.getHeader("Access-Control-Allow-Origin"))
            assertEquals("true", accepted.getHeader("Access-Control-Allow-Credentials"))
            assertTrue(accepted.getHeader("Access-Control-Allow-Headers")!!.contains("x-xsrf-token", ignoreCase = true))
            assertEquals(403, preflight(rejected).status)
        }
    }

    private data class Csrf(val cookie: Cookie, val token: String)

    private fun csrf(vararg cookies: Cookie): Csrf {
        val request = get("/auth/session/csrf")
        if (cookies.isNotEmpty()) request.cookie(*cookies)
        val result = mvc.perform(request)
            .andExpect(status().isOk)
            .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
            .andReturn().response
        return Csrf(assertNotNull(result.getCookie(CSRF_COOKIE)), json.readTree(result.contentAsString)["token"].asText())
    }

    private fun loginCookie(name: String = ACCESS_COOKIE): Cookie {
        val csrf = csrf()
        val result = mvc.perform(post("/auth/session/login").cookie(csrf.cookie).header("X-XSRF-TOKEN", csrf.token)
            .contentType(MediaType.APPLICATION_JSON).content(credentials))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.token").doesNotExist())
            .andExpect(jsonPath("$.selectionToken").doesNotExist())
            .andExpect(cookie().httpOnly(name, true))
            .andExpect(cookie().secure(name, true))
            .andExpect(cookie().path(name, "/"))
            .andReturn().response
        val cookie = assertNotNull(result.getCookie(name))
        assertTrue(cookie.maxAge > 0)
        assertTrue(result.getHeaders("Set-Cookie").any { it.startsWith("$name=") && it.contains("SameSite=Lax") && !it.contains("Domain=") })
        return cookie
    }

    private val credentials = """{"username":"test","password":"password123"}"""
}
