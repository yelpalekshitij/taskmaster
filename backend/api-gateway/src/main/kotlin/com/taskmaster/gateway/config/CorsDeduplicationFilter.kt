package com.taskmaster.gateway.config

import org.reactivestreams.Publisher
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.cloud.gateway.filter.NettyWriteResponseFilter
import org.springframework.core.Ordered
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.HttpHeaders
import org.springframework.http.server.reactive.ServerHttpResponseDecorator
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

/**
 * Deduplicates CORS response headers at the moment the response is written.
 *
 * Spring Security's CorsWebFilter (a WebFilter) runs before the gateway routing pipeline and
 * adds Access-Control-Allow-Origin to the exchange response. NettyWriteResponseFilter then
 * copies the downstream service's response headers (which may also include CORS headers added
 * by the service's own Spring Security) into the same exchange response. This leaves duplicate
 * CORS headers that the browser rejects.
 *
 * By wrapping the response in a ServerHttpResponseDecorator and intercepting writeWith(), we
 * deduplicate CORS headers at the single moment all sources have contributed — right before
 * applyHeaders() commits them to the network.
 */
@Component
class CorsDeduplicationFilter : GlobalFilter, Ordered {

    override fun getOrder(): Int = NettyWriteResponseFilter.WRITE_RESPONSE_FILTER_ORDER - 1

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val deduped = object : ServerHttpResponseDecorator(exchange.response) {
            override fun writeWith(body: Publisher<out DataBuffer>): Mono<Void> {
                deduplicateCorsHeaders(headers)
                return super.writeWith(body)
            }

            override fun writeAndFlushWith(body: Publisher<out Publisher<out DataBuffer>>): Mono<Void> {
                deduplicateCorsHeaders(headers)
                return super.writeAndFlushWith(body)
            }
        }
        return chain.filter(exchange.mutate().response(deduped).build())
    }

    private fun deduplicateCorsHeaders(headers: HttpHeaders) {
        listOf(
            HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
            HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS,
            HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
            HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
            HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
            HttpHeaders.ACCESS_CONTROL_MAX_AGE,
        ).forEach { name ->
            val values = headers[name]
            if (!values.isNullOrEmpty() && values.size > 1) {
                headers.set(name, values.first())
            }
        }
    }
}
