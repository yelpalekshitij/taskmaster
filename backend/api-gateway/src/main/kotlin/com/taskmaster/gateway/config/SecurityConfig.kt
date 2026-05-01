package com.taskmaster.gateway.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter
import org.springframework.security.oauth2.server.resource.authentication.JwtReactiveAuthenticationManager
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsConfigurationSource
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource
import reactor.core.publisher.Mono

@Configuration
@EnableWebFluxSecurity
class SecurityConfig {

    @Value("\${keycloak.realms.admin.jwk-set-uri:http://keycloak:8180/realms/taskmaster-admin/protocol/openid-connect/certs}")
    private lateinit var adminJwkUri: String

    @Value("\${keycloak.realms.app.jwk-set-uri:http://keycloak:8180/realms/taskmaster-app/protocol/openid-connect/certs}")
    private lateinit var appJwkUri: String

    @Bean
    fun springSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .authorizeExchange { auth ->
                auth
                    .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .pathMatchers(
                        "/actuator/health",
                        "/actuator/info",
                        "/actuator/prometheus"
                    ).permitAll()
                    .pathMatchers(
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/webjars/**"
                    ).permitAll()
                    .anyExchange().authenticated()
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.authenticationManagerResolver { _ ->
                    Mono.just(multiRealmAuthenticationManager())
                }
            }
            .build()
    }

    /**
     * Multi-issuer authentication manager that attempts the admin realm decoder first
     * and falls back to the app realm decoder. This supports tokens issued from both
     * the taskmaster-admin and taskmaster-app Keycloak realms.
     */
    @Bean
    fun multiRealmAuthenticationManager(): ReactiveAuthenticationManager {
        val jwtConverter = buildJwtConverter()

        val adminDecoder = NimbusReactiveJwtDecoder.withJwkSetUri(adminJwkUri).build()
        val appDecoder = NimbusReactiveJwtDecoder.withJwkSetUri(appJwkUri).build()

        val adminManager = JwtReactiveAuthenticationManager(adminDecoder).apply {
            setJwtAuthenticationConverter(ReactiveJwtAuthenticationConverterAdapter(jwtConverter))
        }
        val appManager = JwtReactiveAuthenticationManager(appDecoder).apply {
            setJwtAuthenticationConverter(ReactiveJwtAuthenticationConverterAdapter(jwtConverter))
        }

        return ReactiveAuthenticationManager { authentication ->
            adminManager.authenticate(authentication)
                .onErrorResume { appManager.authenticate(authentication) }
        }
    }

    private fun buildJwtConverter(): JwtAuthenticationConverter {
        val grantedAuthoritiesConverter = JwtGrantedAuthoritiesConverter().apply {
            setAuthoritiesClaimName("realm_access.roles")
            setAuthorityPrefix("ROLE_")
        }
        return JwtAuthenticationConverter().apply {
            setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter)
        }
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
}
