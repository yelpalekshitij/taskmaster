package com.taskmaster.user.common

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Service
import java.util.UUID

@Service("sec")
class SecurityService {

    fun isTenantOwner(tenantId: UUID): Boolean {
        if (isMasterAdmin()) return true
        val ctxTenantId = TenantContext.get()
        return ctxTenantId != null && ctxTenantId == tenantId.toString()
    }

    fun isMasterAdmin(): Boolean = SecurityUtils.hasRole("MASTER_ADMIN")

    fun isSameUser(userId: UUID): Boolean {
        val currentId = SecurityUtils.getCurrentUserId() ?: return false
        return currentId == userId.toString()
    }

    fun hasPermission(permission: String): Boolean {
        val auth = SecurityContextHolder.getContext().authentication
        if (auth !is JwtAuthenticationToken) return false
        val roles = auth.authorities.map { it.authority }
        return when {
            "ROLE_MASTER_ADMIN" in roles || "MASTER_ADMIN" in roles -> true
            else -> {
                val permissions = auth.token.getClaim<List<String>>("permissions") ?: emptyList()
                permission in permissions
            }
        }
    }

    fun canAccessTenant(tenantId: UUID?): Boolean {
        if (tenantId == null) return isMasterAdmin()
        return isTenantOwner(tenantId)
    }
}
