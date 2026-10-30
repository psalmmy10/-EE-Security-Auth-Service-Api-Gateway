package com.pay.gateway.filter;

import com.pay.gateway.constant.GatewayConstants;
import com.pay.gateway.util.RequestHeaders;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Downstream services (server.forward-headers-strategy=framework) treat X-Forwarded-For as the client IP —
 * their rate limits and audit logs depend on it. So the gateway strips EVERY client-supplied forwarding
 * header and sets exactly one trusted value of each. Spring Cloud Gateway's own X-Forwarded handling is
 * disabled in properties because it appends to (and so preserves) client-supplied values.
 */
@Component
public class ForwardedHeadersSanitizingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String clientIp = exchange.getAttribute(GatewayConstants.ATTR_CLIENT_IP);
        String clientProto = exchange.getAttribute(GatewayConstants.ATTR_CLIENT_PROTO);

        // Work on a writable copy: headers of an already-mutated request can be read-only
        ServerWebExchange sanitized = RequestHeaders.modify(exchange, h -> {
            GatewayConstants.UNTRUSTED_INBOUND_HEADERS.forEach(h::remove);
            if (clientIp != null) {
                h.set("X-Forwarded-For", clientIp);
            }
            if (clientProto != null) {
                h.set("X-Forwarded-Proto", clientProto);
            }
        });
        return chain.filter(sanitized);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }
}