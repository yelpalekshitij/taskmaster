package com.taskmaster.gateway.filter

import io.micrometer.tracing.Tracer
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.util.UUID

/**
 * Global logging filter that records every inbound request and its eventual response.
 *
 * For each request it:
 *  1. Captures start time so it can compute latency.
 *  2. Populates MDC keys (traceId, spanId, requestId, method, path) before delegating
 *     to the next filter in the chain so all downstream log lines share these fields.
 *  3. Adds an after-completion hook (via [ServerHttpResponse.beforeCommit]) that logs
 *     the HTTP status and elapsed milliseconds once the response has been written.
 *  4. Clears MDC on termination to prevent context leakage across reactive threads.
 *
 * Order -2 places it just before [TenantHeaderGatewayFilter] so the tenant context is
 * available if needed, but still early enough to wrap the full request lifecycle.
 */
@Component
class RequestLoggingFilter(private val tracer: Tracer) : GlobalFilter, Ordered {

    private val log = LoggerFactory.getLogger(RequestLoggingFilter::class.java)

    override fun getOrder(): Int = -2

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val startTime = System.currentTimeMillis()
        val request = exchange.request
        val method = request.method.name()
        val path = request.uri.path
        val requestId = request.headers.getFirst("X-Request-ID") ?: UUID.randomUUID().toString()

        populateMdc(requestId, method, path)

        // Forward the request-ID header downstream so callers can correlate logs
        val mutatedExchange = exchange.mutate()
            .request(exchange.request.mutate().header("X-Request-ID", requestId).build())
            .build()

        mutatedExchange.response.beforeCommit {
            val status = mutatedExchange.response.statusCode?.value() ?: 0
            val duration = System.currentTimeMillis() - startTime
            log.info(
                "REQUEST method={} path={} status={} duration={}ms requestId={}",
                method, path, status, duration, requestId
            )
            Mono.empty()
        }

        return chain.filter(mutatedExchange)
            .doFinally { clearMdc() }
    }

    private fun populateMdc(requestId: String, method: String, path: String) {
        tracer.currentSpan()?.context()?.let { ctx ->
            MDC.put("traceId", ctx.traceId())
            MDC.put("spanId", ctx.spanId())
        }
        MDC.put("requestId", requestId)
        MDC.put("httpMethod", method)
        MDC.put("httpPath", path)
    }

    private fun clearMdc() {
        MDC.remove("traceId")
        MDC.remove("spanId")
        MDC.remove("requestId")
        MDC.remove("httpMethod")
        MDC.remove("httpPath")
    }
}
