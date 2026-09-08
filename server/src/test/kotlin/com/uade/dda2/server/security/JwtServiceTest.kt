package com.uade.dda2.server.security

import com.uade.dda2.server.config.JwtProperties
import com.uade.dda2.server.feature.auth.entity.Permission
import com.uade.dda2.server.feature.auth.entity.Role
import com.uade.dda2.server.feature.auth.entity.User
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class JwtServiceTest {
    private val properties = JwtProperties(secret = "test-only-signing-key-at-least-32-bytes-long")
    private val service = JwtService(properties)
    private val citizen = Role(1, "CIUDADANO")
    private val auditor = Role(2, "AUDITOR", mutableSetOf(Permission(1, "roles:view")))
    private val user = User(id = 42, roles = mutableSetOf(citizen, auditor))

    @Test
    fun `cada token tiene solo los permisos de un rol y admite username ausente`() {
        val principal = service.parse(service.createToken(user, citizen))
        assertEquals(42, principal.id)
        assertEquals("CIUDADANO", principal.activeRole)
        assertEquals(emptyList(), principal.permissions)
        assertNull(principal.username)
        assertEquals(listOf("roles:view"), service.parse(service.createToken(user, auditor)).permissions)
    }

    @Test
    fun `los tokens de seleccion y operativos no son intercambiables`() {
        val selectionToken = service.createRoleSelectionToken(user)
        assertEquals(42, service.parseRoleSelectionToken(selectionToken))
        assertFailsWith<JwtException> { service.parse(selectionToken) }
        assertFailsWith<JwtException> { service.parseRoleSelectionToken(service.createToken(user, citizen)) }
    }

    @Test
    fun `rechaza un token anterior sin proposito ni rol activo`() {
        val token = signed(mapOf("roles" to listOf("AUDITOR"), "permissions" to listOf("roles:view")))
        assertFailsWith<JwtException> { service.parse(token) }
    }

    @Test
    fun `rechaza tokens sin vencimiento y claims de autorizacion incompletos`() {
        assertFailsWith<IllegalArgumentException> { service.parse(signed(accessClaims(), expires = false)) }
        assertFailsWith<IllegalArgumentException> { service.parse(signed(accessClaims() - "active_role")) }
        assertFailsWith<IllegalArgumentException> { service.parse(signed(accessClaims() + ("permissions" to listOf(123)))) }
        assertFailsWith<IllegalArgumentException> { service.parse(signed(accessClaims(), subject = "0")) }
    }

    @Test
    fun `rechaza tokens vencidos o firmados por otro emisor o clave`() {
        assertFailsWith<JwtException> { service.parse(signed(accessClaims(), seconds = -10)) }
        assertFailsWith<JwtException> {
            service.parseRoleSelectionToken(signed(mapOf("token_type" to "ROLE_SELECTION"), seconds = -10))
        }
        assertFailsWith<JwtException> { service.parse(signed(accessClaims(), issuer = "other")) }
        val otherService = JwtService(properties.copy(secret = "another-test-key-not-shared-with-the-server"))
        assertFailsWith<JwtException> { service.parse(otherService.createToken(user, citizen)) }
    }

    @Test
    fun `no emite tokens con un rol ajeno o usuario inactivo`() {
        assertFailsWith<IllegalArgumentException> { service.createToken(user, Role(3, "ADMIN")) }
        val suppliedRole = Role(citizen.id, citizen.name, mutableSetOf(Permission(3, "users:delete")))
        assertEquals(emptyList(), service.parse(service.createToken(user, suppliedRole)).permissions)
        user.active = false
        assertFailsWith<IllegalArgumentException> { service.createToken(user, citizen) }
    }

    @Test
    fun `requiere una clave propia y tiempos de vida positivos`() {
        assertFailsWith<IllegalArgumentException> { JwtService(JwtProperties()) }
        assertFailsWith<IllegalArgumentException> { JwtService(properties.copy(secret = "change-me-change-me-change-me-change-me-32chars")) }
        assertFailsWith<IllegalArgumentException> { JwtService(properties.copy(roleSelectionExpirationSeconds = 0)) }
    }

    private fun accessClaims(): Map<String, Any> =
        mapOf("token_type" to "ACCESS", "active_role" to "AUDITOR", "permissions" to listOf("roles:view"))

    private fun signed(
        claims: Map<String, Any>,
        expires: Boolean = true,
        seconds: Long = 60,
        subject: String = "42",
        issuer: String = properties.issuer,
    ): String {
        val now = Instant.now()
        val builder = Jwts.builder().subject(subject).issuer(issuer)
            .issuedAt(Date.from(now.minusSeconds(60))).claims(claims)
        if (expires) builder.expiration(Date.from(now.plusSeconds(seconds)))
        return builder.signWith(Keys.hmacShaKeyFor(properties.secret.toByteArray()), Jwts.SIG.HS256).compact()
    }
}
