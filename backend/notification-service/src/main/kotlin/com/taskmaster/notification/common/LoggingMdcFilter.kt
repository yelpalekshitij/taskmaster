package com.taskmaster.notification.common

import io.micrometer.tracing.Tracer
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.core.annotation.Order
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

@Component
@Order(2)
class LoggingMdcFilter(private val tracer: Tracer) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            val span = tracer.currentSpan()
            span?.context()?.let { ctx ->
                MDC.put("traceId", ctx.traceId())
                MDC.put("spanId", ctx.spanId())
            }

            val requestId = request.getHeader("X-Request-ID") ?: UUID.randomUUID().toString()
            MDC.put("requestId", requestId)
            MDC.put("userAgent", request.getHeader("User-Agent") ?: "unknown")
            MDC.put("httpMethod", request.method)
            MDC.put("httpPath", request.requestURI)

            TenantContext.get()?.let { MDC.put("tenantId", it) }

            val authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext().authentication
            if (authentication is JwtAuthenticationToken) {
                authentication.token.subject?.let { MDC.put("userId", it) }
            }

            response.setHeader("X-Request-ID", requestId)

            filterChain.doFilter(request, response)
        } finally {
            MDC.clear()
        }
    }
}
