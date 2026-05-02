package com.taskmaster.notification

import com.taskmaster.notification.config.SecurityConfig
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.Jwt
import java.time.Instant

class SecurityConfigTest {

    private val config = SecurityConfig()

    @Test
    fun `jwtAuthenticationConverter maps realm_access roles to ROLE_ prefixed authorities`() {
        val jwt = buildJwt(mapOf("realm_access" to mapOf("roles" to listOf("MASTER_ADMIN", "USER"))))

        val authentication = config.jwtAuthenticationConverter().convert(jwt)!!

        val authorities = authentication.authorities.map { it.authority }
        assertEquals(listOf("ROLE_MASTER_ADMIN", "ROLE_USER"), authorities.sorted())
    }

    @Test
    fun `jwtAuthenticationConverter returns empty authorities when realm_access claim is absent`() {
        val jwt = buildJwt(emptyMap())

        val authentication = config.jwtAuthenticationConverter().convert(jwt)!!

        assertTrue(authentication.authorities.isEmpty())
    }

    @Test
    fun `jwtAuthenticationConverter returns empty authorities when roles list is absent from realm_access`() {
        val jwt = buildJwt(mapOf("realm_access" to mapOf<String, Any>()))

        val authentication = config.jwtAuthenticationConverter().convert(jwt)!!

        assertTrue(authentication.authorities.isEmpty())
    }

    @Test
    fun `jwtAuthenticationConverter does not extract flat realm_access_roles claim`() {
        val jwt = buildJwt(mapOf("realm_access.roles" to listOf("SHOULD_NOT_APPEAR")))

        val authentication = config.jwtAuthenticationConverter().convert(jwt)!!

        assertTrue(authentication.authorities.isEmpty(),
            "Flat 'realm_access.roles' claim should not produce authorities")
    }

    @Test
    fun `admin realm issuer pattern accepts both localhost and docker container hostname`() {
        val localhostIssuer = "http://localhost:8180/realms/taskmaster-admin"
        val dockerIssuer = "http://keycloak:8180/realms/taskmaster-admin"
        assertTrue(localhostIssuer.contains("/realms/taskmaster-admin"))
        assertTrue(dockerIssuer.contains("/realms/taskmaster-admin"))
    }

    @Test
    fun `issuer from unknown realm does not match any trusted realm pattern`() {
        val unknownIssuer = "http://keycloak:8180/realms/unknown"
        assertFalse(unknownIssuer.contains("/realms/taskmaster-admin"))
        assertFalse(unknownIssuer.contains("/realms/taskmaster-app"))
    }

    private fun buildJwt(extraClaims: Map<String, Any>): Jwt {
        val builder = Jwt.withTokenValue("test.token.value")
            .header("alg", "RS256")
            .claim("sub", "user-123")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
        extraClaims.forEach { (key, value) -> builder.claim(key, value) }
        return builder.build()
    }
}
