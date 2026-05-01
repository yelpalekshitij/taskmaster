package com.taskmaster.user.keycloak

import org.keycloak.admin.client.Keycloak
import org.keycloak.admin.client.KeycloakBuilder
import org.keycloak.representations.idm.CredentialRepresentation
import org.keycloak.representations.idm.UserRepresentation
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class KeycloakAdminService(
    @Value("\${keycloak.admin.server-url:http://keycloak:8180}") private val serverUrl: String,
    @Value("\${keycloak.admin.realm:master}") private val adminRealm: String,
    @Value("\${keycloak.admin.client-id:admin-cli}") private val clientId: String,
    @Value("\${keycloak.admin.username:admin}") private val adminUsername: String,
    @Value("\${keycloak.admin.password:admin}") private val adminPassword: String
) {
    private val log = LoggerFactory.getLogger(KeycloakAdminService::class.java)

    private fun keycloak(): Keycloak = KeycloakBuilder.builder()
        .serverUrl(serverUrl)
        .realm(adminRealm)
        .clientId(clientId)
        .username(adminUsername)
        .password(adminPassword)
        .build()

    fun createTenantAdmin(
        tenantId: UUID,
        email: String,
        username: String,
        firstName: String,
        lastName: String,
        password: String
    ): String = createUser(
        realm = "taskmaster-admin",
        email = email,
        username = username,
        firstName = firstName,
        lastName = lastName,
        password = password,
        tenantId = tenantId,
        roleName = "TENANT_ADMIN"
    )

    fun createAppUser(
        tenantId: UUID,
        email: String,
        username: String,
        firstName: String,
        lastName: String,
        password: String
    ): String = createUser(
        realm = "taskmaster-app",
        email = email,
        username = username,
        firstName = firstName,
        lastName = lastName,
        password = password,
        tenantId = tenantId,
        roleName = "USER"
    )

    private fun createUser(
        realm: String,
        email: String,
        username: String,
        firstName: String,
        lastName: String,
        password: String,
        tenantId: UUID,
        roleName: String
    ): String {
        val kc = keycloak()
        try {
            val credential = CredentialRepresentation().apply {
                type = CredentialRepresentation.PASSWORD
                value = password
                isTemporary = false
            }

            val user = UserRepresentation().apply {
                this.username = username
                this.email = email
                this.firstName = firstName
                this.lastName = lastName
                isEnabled = true
                isEmailVerified = true
                credentials = listOf(credential)
                attributes = mapOf("tenant_id" to listOf(tenantId.toString()))
            }

            val realmResource = kc.realm(realm)
            val response = realmResource.users().create(user)

            if (response.status !in 200..201) {
                throw RuntimeException("Failed to create Keycloak user. Status: ${response.status}")
            }

            val userId = response.location.path.substringAfterLast("/")

            val role = realmResource.roles().get(roleName).toRepresentation()
            realmResource.users().get(userId).roles().realmLevel().add(listOf(role))

            log.info("Keycloak user created: realm={}, userId={}, role={}", realm, userId, roleName)
            return userId
        } finally {
            kc.close()
        }
    }

    fun deactivateUser(realm: String, keycloakId: String) {
        val kc = keycloak()
        try {
            val user = kc.realm(realm).users().get(keycloakId).toRepresentation()
            user.isEnabled = false
            kc.realm(realm).users().get(keycloakId).update(user)
            log.info("Keycloak user deactivated: realm={}, keycloakId={}", realm, keycloakId)
        } finally {
            kc.close()
        }
    }
}
