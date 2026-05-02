package com.taskmaster.gateway.config

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.Jwt
import java.time.Instant

class SecurityConfigTest {

    private val config = SecurityConfig()

    @Test
    fun `buildJwtConverter maps realm_access roles to ROLE_ prefixed authorities`() {
        val jwt = buildJwt(mapOf("realm_access" to mapOf("roles" to listOf("MASTER_ADMIN", "USER"))))

        val converter = config.buildJwtConverter()
        val authentication = converter.convert(jwt)!!

        val authorities = authentication.authorities.map { it.authority }
        assertEquals(listOf("ROLE_MASTER_ADMIN", "ROLE_USER"), authorities.sorted())
    }

    @Test
    fun `buildJwtConverter returns empty authorities when realm_access claim is absent`() {
        val jwt = buildJwt(emptyMap())

        val authentication = config.buildJwtConverter().convert(jwt)!!

        assertTrue(authentication.authorities.isEmpty())
    }

    @Test
    fun `buildJwtConverter returns empty authorities when roles list is absent from realm_access`() {
        val jwt = buildJwt(mapOf("realm_access" to mapOf<String, Any>()))

        val authentication = config.buildJwtConverter().convert(jwt)!!

        assertTrue(authentication.authorities.isEmpty())
    }

    @Test
    fun `buildJwtConverter does not extract flat realm_access_roles claim`() {
        // This test documents that the OLD broken approach (setAuthoritiesClaimName("realm_access.roles"))
        // is not used — Keycloak stores roles nested, not as a flat dot-separated claim name
        val jwt = buildJwt(mapOf("realm_access.roles" to listOf("SHOULD_NOT_APPEAR")))

        val authentication = config.buildJwtConverter().convert(jwt)!!

        assertTrue(authentication.authorities.isEmpty(),
            "Flat 'realm_access.roles' claim must not produce authorities; only nested realm_access.roles should")
    }

    @Test
    fun `buildJwtConverter handles both admin and app realm roles`() {
        val jwt = buildJwt(mapOf("realm_access" to mapOf("roles" to listOf("TENANT_ADMIN"))))

        val authorities = config.buildJwtConverter().convert(jwt)!!.authorities.map { it.authority }

        assertEquals(listOf("ROLE_TENANT_ADMIN"), authorities)
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
