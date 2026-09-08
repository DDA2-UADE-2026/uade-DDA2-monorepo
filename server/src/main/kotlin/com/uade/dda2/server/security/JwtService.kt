package com.uade.dda2.server.security

import com.uade.dda2.server.config.JwtProperties
import com.uade.dda2.server.feature.auth.entity.Role
import com.uade.dda2.server.feature.auth.entity.User
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Date

@Service
class JwtService(
    private val jwtProperties: JwtProperties,
) {
    private val signingKey = jwtProperties.secret.toByteArray(StandardCharsets.UTF_8).let { secret ->
        require(secret.size >= 32) { "JWT_SECRET must contain at least 32 bytes." }
        require(!jwtProperties.secret.startsWith("change-me")) { "JWT_SECRET must not use the example secret." }
        Keys.hmacShaKeyFor(secret)
    }

    init {
        require(jwtProperties.issuer.isNotBlank()) { "JWT_ISSUER must not be blank." }
        require(jwtProperties.expirationSeconds > 0) { "JWT_EXPIRATION_SECONDS must be positive." }
        require(jwtProperties.roleSelectionExpirationSeconds > 0) { "JWT_ROLE_SELECTION_EXPIRATION_SECONDS must be positive." }
    }

    fun createToken(user: User, activeRole: Role): String {
        require(user.active) { "The user must be active." }
        val assignedRole = user.roles.firstOrNull { it.id == activeRole.id && it.name == activeRole.name }
        require(assignedRole != null && assignedRole.name.isNotBlank()) {
            "The active role must belong to the user."
        }
        return tokenBuilder(user, "ACCESS", jwtProperties.expirationSeconds)
            .claim("username", user.username)
            .claim("active_role", assignedRole.name)
            .claim("permissions", assignedRole.permissions.map { it.name }.distinct().sorted())
            .signWith(signingKey, Jwts.SIG.HS256)
            .compact()
    }

    fun createRoleSelectionToken(user: User): String {
        require(user.active && user.roles.isNotEmpty()) { "An active user with assigned roles is required." }
        return tokenBuilder(user, "ROLE_SELECTION", jwtProperties.roleSelectionExpirationSeconds)
            .signWith(signingKey, Jwts.SIG.HS256)
            .compact()
    }

    // Only ACCESS tokens can become a Spring Security principal.
    fun parse(token: String): JwtPrincipal {
        val claims = parseClaims(token, "ACCESS")
        val permissions = claims["permissions"]
        require(permissions is List<*> && permissions.all { it is String && it.isNotBlank() }) {
            "Invalid permissions claim."
        }
        val activeRole = claims["active_role", String::class.java]
        require(!activeRole.isNullOrBlank()) { "An active role is required." }
        return JwtPrincipal(
            id = userId(claims),
            username = claims["username", String::class.java],
            activeRole = activeRole,
            permissions = permissions.filterIsInstance<String>().distinct().sorted(),
        )
    }

    fun parseRoleSelectionToken(token: String): Long = userId(parseClaims(token, "ROLE_SELECTION"))

    private fun tokenBuilder(user: User, type: String, lifetime: Long) = Instant.now().let { now ->
        Jwts.builder()
            .subject(requireNotNull(user.id).also { require(it > 0) }.toString())
            .issuer(jwtProperties.issuer)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(lifetime)))
            .claim("token_type", type)
    }

    private fun parseClaims(token: String, type: String): Claims {
        val claims = Jwts.parser()
            .verifyWith(signingKey)
            .requireIssuer(jwtProperties.issuer)
            .require("token_type", type)
            .sig().clear().add(Jwts.SIG.HS256).and()
            .build()
            .parseSignedClaims(token)
            .payload
        val issuedAt = claims.issuedAt
        val expiration = claims.expiration
        require(issuedAt != null && expiration != null && expiration.after(issuedAt)) {
            "Issued-at and expiration claims are required."
        }
        userId(claims)
        return claims
    }

    private fun userId(claims: Claims): Long =
        requireNotNull(claims.subject?.toLongOrNull()?.takeIf { it > 0 }) { "Invalid subject." }
}
