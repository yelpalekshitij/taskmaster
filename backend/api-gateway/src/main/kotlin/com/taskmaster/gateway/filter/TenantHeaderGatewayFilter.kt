package com.taskmaster.gateway.filter

import org.slf4j.LoggerFactory
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

/**
 * Global gateway filter that extracts tenant and user information from the validated JWT
 * and injects them as request headers before forwarding to downstream services.
 *
 * Headers added:
 *  - X-Tenant-Id  : the "tenant_id" custom claim from the JWT
 *  - X-User-Id    : the JWT subject (Keycloak user UUID)
 *
 * Order -1 ensures this runs before any route-level filters but after Spring Security.
 */
@Component
class TenantHeaderGatewayFilter : GlobalFilter, Ordered {

    private val log = LoggerFactory.getLogger(TenantHeaderGatewayFilter::class.java)

    override fun getOrder(): Int = -1

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        return exchange.getPrincipal<Authentication>()
            .filter { it is JwtAuthenticationToken }
            .cast(JwtAuthenticationToken::class.java)
            .flatMap { jwtToken ->
                val token = jwtToken.token
                val tenantId = token.getClaim<String?>("tenant_id") ?: ""
                val userId = token.subject ?: ""

                log.debug("Injecting headers — tenantId={}, userId={}", tenantId, userId)

                val mutatedRequest = exchange.request.mutate()
                    .header("X-Tenant-Id", tenantId)
                    .header("X-User-Id", userId)
                    .build()

                chain.filter(exchange.mutate().request(mutatedRequest).build())
            }
            .switchIfEmpty(chain.filter(exchange))
    }
}
