package com.uade.dda2.server.security

import com.uade.dda2.server.config.JwtProperties
import com.uade.dda2.server.feature.auth.entity.User
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.*
import javax.crypto.SecretKey

@Service
class JwtService(
    private val jwtProperties: JwtProperties,
) {
    fun createToken(
        user: User,
        roles: List<String>,
        permissions: List<String>,
    ): String {
        val now = Instant.now()
        val expiresAt = now.plusSeconds(jwtProperties.expirationSeconds)

        return Jwts.builder()
            .subject(requireNotNull(user.id).toString())
            .issuer(jwtProperties.issuer)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiresAt))
            .claim("username", user.username)
            .claim("roles", roles)
            .claim("permissions", permissions)
            .signWith(signingKey())
            .compact()
    }

    fun parse(token: String): JwtPrincipal {
        val claims = Jwts.parser()
            .verifyWith(signingKey())
            .requireIssuer(jwtProperties.issuer)
            .build()
            .parseSignedClaims(token)
            .payload

        return JwtPrincipal(
            id = claims.subject.toLong(),
            username = claims["username", String::class.java],
            roles = stringListClaim(claims["roles"]),
            permissions = stringListClaim(claims["permissions"]),
        )
    }

    private fun signingKey(): SecretKey {
        val secret = jwtProperties.secret.toByteArray(StandardCharsets.UTF_8)
        require(secret.size >= 32) { "JWT_SECRET must contain at least 32 bytes." }
        return Keys.hmacShaKeyFor(secret)
    }

    private fun stringListClaim(value: Any?): List<String> =
        when (value) {
            is List<*> -> value.filterIsInstance<String>()
            is String -> listOf(value)
            else -> emptyList()
        }
}
