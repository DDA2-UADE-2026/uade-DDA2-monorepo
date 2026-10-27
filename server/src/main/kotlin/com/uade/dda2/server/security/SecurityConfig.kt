package com.uade.dda2.server.security

import com.uade.dda2.server.config.CorsProperties
import com.uade.dda2.server.config.AuthCookieProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.csrf.CookieCsrfTokenRepository
import org.springframework.security.web.csrf.CsrfTokenRepository
import org.springframework.security.web.csrf.CsrfException
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val securityErrorResponseWriter: SecurityErrorResponseWriter,
    private val corsProperties: CorsProperties,
    private val csrfTokenRepository: CsrfTokenRepository,
) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain =
        http
            .csrf { csrf ->
                csrf.csrfTokenRepository(csrfTokenRepository)
                csrf.requireCsrfProtectionMatcher { request ->
                    val path = request.requestURI.removePrefix(request.contextPath)
                    val unsafe = request.method !in setOf("GET", "HEAD", "OPTIONS", "TRACE")
                    // Bearer-only clients keep their existing contract. Any write authenticated
                    // by an ambient cookie must prove possession of the CSRF token.
                    unsafe && (path.startsWith("/auth/session/") ||
                        (!request.hasBearerAuthorization() &&
                            request.cookies.orEmpty().any { it.name == ACCESS_COOKIE }))
                }
            }
            .cors { }
            .logout { it.disable() }
            .headers { headers ->
                headers.frameOptions { it.disable() }
                headers.contentSecurityPolicy { csp ->
                    val frameAncestors = (listOf("'self'") + corsProperties.nonBlankOrigins).joinToString(" ")
                    csp.policyDirectives("frame-ancestors $frameAncestors")
                }
            }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .exceptionHandling {
                it.authenticationEntryPoint { request, response, _ ->
                    securityErrorResponseWriter.write(
                        request = request,
                        response = response,
                        status = HttpStatus.UNAUTHORIZED,
                        code = "AUTH_UNAUTHENTICATED",
                        message = "No autenticado.",
                    )
                }
                it.accessDeniedHandler { request, response, exception ->
                    securityErrorResponseWriter.write(
                        request = request,
                        response = response,
                        status = HttpStatus.FORBIDDEN,
                        code = if (exception is CsrfException) "AUTH_CSRF_INVALID" else "AUTH_FORBIDDEN",
                        message = if (exception is CsrfException) "La verificación CSRF es inválida. Reintentá la operación." else "El usuario no posee permiso para realizar esta acción.",
                    )
                }
            }
            .authorizeHttpRequests {
                it.requestMatchers(HttpMethod.POST, "/auth/login", "/auth/select-role").permitAll()
                it.requestMatchers(HttpMethod.POST, "/auth/session/login", "/auth/session/select-role", "/auth/session/logout").permitAll()
                it.requestMatchers(HttpMethod.GET, "/auth/session/csrf", "/auth/session/role-selection").permitAll()
                it.requestMatchers(HttpMethod.GET, "/api/programs", "/api/programs/**").permitAll()
                it.requestMatchers(HttpMethod.GET, "/api/images/**").permitAll()
                it.requestMatchers("/actuator/health", "/error").permitAll()
                it.requestMatchers("/api-docs/**", "/swagger-ui/**").permitAll()
                it.anyRequest().authenticated()
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .build()

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun corsConfigurationSource(corsProperties: CorsProperties): CorsConfigurationSource {
        val configuration = CorsConfiguration().apply {
            allowedOrigins = corsProperties.nonBlankOrigins
            allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            allowedHeaders = listOf("Authorization", "Content-Type", "Idempotency-Key", "X-XSRF-TOKEN")
            exposedHeaders = listOf("Authorization", "Location", "Idempotency-Replayed")
            allowCredentials = true
        }

        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", configuration)
        }
    }
}

@Configuration
class CsrfCookieConfig {
    @Bean
    fun csrfTokenRepository(properties: AuthCookieProperties): CsrfTokenRepository =
        CookieCsrfTokenRepository().apply {
            setCookieName(CSRF_COOKIE)
            setCookiePath("/")
            setCookieCustomizer { cookie ->
                cookie.httpOnly(true).secure(properties.secure).sameSite(properties.sameSite)
            }
        }
}
