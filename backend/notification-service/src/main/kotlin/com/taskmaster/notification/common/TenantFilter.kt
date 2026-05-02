package com.taskmaster.notification.common

import com.nimbusds.jwt.SignedJWT
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
@Order(1)
class TenantFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val tenantId = request.getHeader("X-Tenant-Id")
            ?: extractTenantFromJwt(request.getHeader("Authorization"))
        TenantContext.set(tenantId)
        try {
            filterChain.doFilter(request, response)
        } finally {
            TenantContext.clear()
        }
    }

    private fun extractTenantFromJwt(authHeader: String?): String? {
        if (authHeader == null || !authHeader.startsWith("Bearer ", ignoreCase = true)) return null
        return try {
            SignedJWT.parse(authHeader.substring(7)).jwtClaimsSet.getStringClaim("tenant_id")
        } catch (_: Exception) { null }
    }
}
