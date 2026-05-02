package com.taskmaster.gateway.config

import com.nimbusds.jwt.SignedJWT
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.authentication.JwtReactiveAuthenticationManager
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsConfigurationSource
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource
import reactor.core.publisher.Mono
import java.util.concurrent.ConcurrentHashMap

@Configuration
@EnableWebFluxSecurity
class SecurityConfig {

    @Value("\${keycloak.realms.admin.jwk-set-uri:http://keycloak:8180/realms/taskmaster-admin/protocol/openid-connect/certs}")
    private lateinit var adminJwkUri: String

    @Value("\${keycloak.realms.app.jwk-set-uri:http://keycloak:8180/realms/taskmaster-app/protocol/openid-connect/certs}")
    private lateinit var appJwkUri: String

    // One ReactiveAuthenticationManager per distinct issuer value seen in JWTs (same pattern as servlet services).
    // localhost:8180 and keycloak:8180 tokens each get their own cached manager,
    // while JWKS is always fetched from the Docker-internal hostname.
    private val authManagerCache = ConcurrentHashMap<String, ReactiveAuthenticationManager>()

    @Bean
    fun springSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .authorizeExchange { auth ->
                auth
                    .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .pathMatchers("/actuator/**").permitAll()
                    .pathMatchers(
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/webjars/**"
                    ).permitAll()
                    .anyExchange().authenticated()
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.authenticationManagerResolver { exchange ->
                    val authHeader = exchange.request.headers.getFirst("Authorization")
                        ?: return@authenticationManagerResolver Mono.empty()
                    if (!authHeader.startsWith("Bearer ", ignoreCase = true)) {
                        return@authenticationManagerResolver Mono.empty()
                    }
                    val rawToken = authHeader.substring(7)
                    val issuer = try {
                        SignedJWT.parse(rawToken).jwtClaimsSet.issuer
                    } catch (_: Exception) {
                        return@authenticationManagerResolver Mono.empty()
                    } ?: return@authenticationManagerResolver Mono.empty()

                    try {
                        Mono.just(authManagerCache.getOrPut(issuer) { buildReactiveAuthManager(issuer) })
                    } catch (_: IllegalArgumentException) {
                        Mono.empty()
                    }
                }
            }
            .build()
    }

    private fun buildReactiveAuthManager(claimedIssuer: String): ReactiveAuthenticationManager {
        val jwksUri = when {
            claimedIssuer.contains("/realms/taskmaster-admin") -> adminJwkUri
            claimedIssuer.contains("/realms/taskmaster-app") -> appJwkUri
            else -> throw IllegalArgumentException("Untrusted issuer: $claimedIssuer")
        }
        val decoder = NimbusReactiveJwtDecoder.withJwkSetUri(jwksUri).build()
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(claimedIssuer))
        return JwtReactiveAuthenticationManager(decoder).apply {
            setJwtAuthenticationConverter(ReactiveJwtAuthenticationConverterAdapter(buildJwtConverter()))
        }
    }

    fun buildJwtConverter(): JwtAuthenticationConverter {
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

    private fun corsConfigurationSource(): CorsConfigurationSource {
        val config = CorsConfiguration().apply {
            allowedOriginPatterns = listOf("*")
            allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            allowedHeaders = listOf("*")
            allowCredentials = true
            maxAge = 3600L
        }
        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", config)
        }
    }
}
