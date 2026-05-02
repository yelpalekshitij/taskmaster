package com.taskmaster.user.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerAuthenticationManagerResolver
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
class SecurityConfig {

    @Value("\${keycloak.realms.admin.issuer-uri:http://keycloak:8180/realms/taskmaster-admin}")
    private lateinit var adminIssuerUri: String

    @Value("\${keycloak.realms.app.issuer-uri:http://keycloak:8180/realms/taskmaster-app}")
    private lateinit var appIssuerUri: String

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        val authenticationManagerResolver = JwtIssuerAuthenticationManagerResolver(
            adminIssuerUri, appIssuerUri
        )

        http
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/actuator/**").permitAll()
                    .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                    .anyRequest().authenticated()
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.authenticationManagerResolver(authenticationManagerResolver)
            }

        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config = CorsConfiguration().apply {
            // CHANGE (prod): read from CORS_ALLOWED_ORIGINS env var, e.g. "https://app.yourdomain.com,https://admin.yourdomain.com"
            allowedOrigins = listOf("http://localhost:4200", "http://localhost:4201")
            allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            allowedHeaders = listOf("*")
            allowCredentials = true
            maxAge = 3600L
        }
        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", config)
        }
    }

    @Bean
    fun jwtAuthenticationConverter(): JwtAuthenticationConverter {
        val grantedAuthoritiesConverter = JwtGrantedAuthoritiesConverter().apply {
            setAuthoritiesClaimName("realm_access.roles")
            setAuthorityPrefix("ROLE_")
        }
        return JwtAuthenticationConverter().apply {
            setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter)
        }
    }
}
