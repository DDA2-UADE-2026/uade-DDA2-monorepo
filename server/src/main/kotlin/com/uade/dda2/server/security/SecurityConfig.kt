package com.uade.dda2.server.security

import com.uade.dda2.server.config.CorsProperties
import com.uade.dda2.server.config.JwtProperties
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
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val securityErrorResponseWriter: SecurityErrorResponseWriter,
    private val jwtProperties: JwtProperties,
) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain =
        http
            .csrf {
                // Double-submit cookie: XSRF-TOKEN (legible por JS) + header X-XSRF-TOKEN.
                // Único mecanismo de auth ahora es la cookie httpOnly del JWT, así que
                // cualquier request que mute estado necesita este segundo valor que un
                // sitio atacante no puede leer ni forjar.
                val cookieRepository = CookieCsrfTokenRepository.withHttpOnlyFalse().apply {
                    setCookieCustomizer { cookie -> cookie.secure(jwtProperties.cookieSecure).sameSite("Lax") }
                }
                it.csrfTokenRepository(cookieRepository)
                it.csrfTokenRequestHandler(CsrfTokenRequestAttributeHandler())
            }
            .cors { }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .exceptionHandling {
                it.authenticationEntryPoint { request, response, _ ->
                    securityErrorResponseWriter.write(
                        request = request,
                        response = response,
                        status = HttpStatus.UNAUTHORIZED,
                        code = "AUTH_UNAUTHENTICATED",
                        message = "Unauthenticated.",
                    )
                }
                it.accessDeniedHandler { request, response, _ ->
                    securityErrorResponseWriter.write(
                        request = request,
                        response = response,
                        status = HttpStatus.FORBIDDEN,
                        code = "AUTH_FORBIDDEN",
                        message = "The user does not have permission to perform this action.",
                    )
                }
            }
            .authorizeHttpRequests {
                it.requestMatchers(HttpMethod.POST, "/auth/login", "/auth/logout").permitAll()
                it.requestMatchers(HttpMethod.GET, "/auth/csrf").permitAll()
                it.requestMatchers("/actuator/health", "/error").permitAll()
                it.anyRequest().authenticated()
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .build()

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun corsConfigurationSource(corsProperties: CorsProperties): CorsConfigurationSource {
        val configuration = CorsConfiguration().apply {
            allowedOrigins = corsProperties.allowedOrigins
            allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            allowedHeaders = listOf("Content-Type", "X-XSRF-TOKEN")
            // true: el front principal manda/recibe la cookie httpOnly con el JWT.
            // Requiere allowedOrigins explícitos (ya lo son) — no puede ser "*" con credentials.
            allowCredentials = true
        }

        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", configuration)
        }
    }
}
