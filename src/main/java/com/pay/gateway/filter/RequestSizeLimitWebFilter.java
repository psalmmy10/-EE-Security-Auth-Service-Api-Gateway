package com.pay.gateway.filter;

import com.pay.gateway.config.properties.GatewayProperties;
import com.pay.gateway.constant.enums.GatewayErrorCode;
import com.pay.gateway.util.JsonErrorWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Rejects oversized bodies before they reach any service. Auth payloads (even E2E-encrypted) are a few KB,
 * so a small limit blocks memory-exhaustion attempts cheaply. Bodies are never read or logged here.
 */
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class RequestSizeLimitWebFilter implements WebFilter {

    private final GatewayProperties properties;
    private final JsonErrorWriter errorWriter;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        long length = exchange.getRequest().getHeaders().getContentLength();
        if (length > properties.maxRequestSize().toBytes()) {
            return errorWriter.write(exchange, GatewayErrorCode.PAYLOAD_TOO_LARGE);
        }
        return chain.filter(exchange);
    }
}
