package com.waviewer.api.infrastructure.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.filter.OncePerRequestFilter
import java.security.MessageDigest

// Modelo de seguridad:
//  - /api/v1/webhook: público (Meta lo llama); autenticidad por firma HMAC.
//  - Resto de /api/v1: API key en header X-Api-Key (la app iOS).
//  - Actuator health y Swagger: públicos para infraestructura/desarrollo.
@Configuration
@EnableWebSecurity
class SecurityConfig(private val viewerProps: ViewerProperties) {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.requestMatchers(
                    "/api/v1/webhook/**",
                    "/actuator/health",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                ).permitAll()
                    .anyRequest().permitAll() // la autorización real la hace ApiKeyFilter
            }
            .addFilterBefore(ApiKeyFilter(viewerProps.apiKey), UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }
}

class ApiKeyFilter(private val apiKey: String) : OncePerRequestFilter() {

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI
        return !path.startsWith("/api/v1") || path.startsWith("/api/v1/webhook")
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val provided = request.getHeader("X-Api-Key") ?: ""
        val valid = provided.isNotEmpty() &&
            MessageDigest.isEqual(provided.toByteArray(), apiKey.toByteArray())
        if (!valid) {
            response.status = HttpServletResponse.SC_UNAUTHORIZED
            response.contentType = "application/json"
            response.writer.write("""{"error":"API key inválida o ausente"}""")
            return
        }
        filterChain.doFilter(request, response)
    }
}
