package com.taskmaster.task.common

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import java.util.UUID

object SecurityUtils {

    fun getCurrentUserId(): String? {
        val auth = SecurityContextHolder.getContext().authentication
        return if (auth is JwtAuthenticationToken) auth.token.subject else null
    }

    fun getCurrentTenantId(): String? = TenantContext.get()

    fun getCurrentTenantIdAsUUID(): UUID? {
        return TenantContext.get()?.let {
            if (it.isBlank()) null else UUID.fromString(it)
        }
    }

    fun hasRole(role: String): Boolean {
        val auth = SecurityContextHolder.getContext().authentication ?: return false
        return auth.authorities.any { it.authority == "ROLE_$role" || it.authority == role }
    }

    fun isMasterAdmin(): Boolean = hasRole("MASTER_ADMIN")

    fun getJwtClaim(claim: String): Any? {
        val auth = SecurityContextHolder.getContext().authentication
        return if (auth is JwtAuthenticationToken) auth.token.getClaim(claim) else null
    }
}
