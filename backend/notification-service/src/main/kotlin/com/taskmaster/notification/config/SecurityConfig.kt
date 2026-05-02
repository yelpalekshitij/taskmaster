package com.taskmaster.notification.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.AuthenticationManagerResolver
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerAuthenticationManagerResolver
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import java.util.concurrent.ConcurrentHashMap

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
class SecurityConfig {

    @Value("\${keycloak.realms.admin.issuer-uri:http://keycloak:8180/realms/taskmaster-admin}")
    private lateinit var adminIssuerUri: String

    @Value("\${keycloak.realms.app.issuer-uri:http://keycloak:8180/realms/taskmaster-app}")
    private lateinit var appIssuerUri: String

    private val authManagerCache = ConcurrentHashMap<String, AuthenticationManager>()

    private fun buildAuthManager(claimedIssuer: String, dockerBaseUri: String): AuthenticationManager {
        val decoder = NimbusJwtDecoder
            .withJwkSetUri("$dockerBaseUri/protocol/openid-connect/certs")
            .build()
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(claimedIssuer))
        val provider = JwtAuthenticationProvider(decoder)
        provider.setJwtAuthenticationConverter(jwtAuthenticationConverter())
        return AuthenticationManager { auth -> provider.authenticate(auth) }
    }

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        val resolver = AuthenticationManagerResolver<String> { issuer ->
            authManagerCache.getOrPut(issuer) {
                when {
                    issuer.contains("/realms/taskmaster-admin") -> buildAuthManager(issuer, adminIssuerUri)
                    issuer.contains("/realms/taskmaster-app")   -> buildAuthManager(issuer, appIssuerUri)
                    else -> throw IllegalArgumentException("Untrusted issuer: $issuer")
                }
            }
        }

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
                oauth2.authenticationManagerResolver(JwtIssuerAuthenticationManagerResolver(resolver))
            }

        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config = CorsConfiguration().apply {
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
        return JwtAuthenticationConverter().apply {
            setJwtGrantedAuthoritiesConverter { jwt ->
                @Suppress("UNCHECKED_CAST")
                val roles = (jwt.getClaim<Map<String, Any>>("realm_access")
                    ?.get("roles") as? Collection<String>)
                    ?: emptyList()
                roles.map { SimpleGrantedAuthority("ROLE_$it") }
            }
        }
    }
}
