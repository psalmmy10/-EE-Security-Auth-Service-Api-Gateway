package com.pay.gateway.filter;

import com.pay.gateway.constant.GatewayConstants;
import com.pay.gateway.util.ClientIpResolver;
import com.pay.gateway.util.RequestHeaders;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Runs first, before Spring Security. Assigns the correlation id that follows the request to every
 * downstream service (same header the auth service already uses), and resolves the client IP/protocol once.
 */
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdWebFilter implements WebFilter {

    private static final Pattern SAFE_ID = Pattern.compile("^[A-Za-z0-9\\-]{8,64}$");

    private final ClientIpResolver clientIpResolver;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String inbound = exchange.getRequest().getHeaders().getFirst(GatewayConstants.HEADER_CORRELATION_ID);
        String correlationId = inbound != null && SAFE_ID.matcher(inbound).matches() ? inbound : UUID.randomUUID().toString();

        ServerWebExchange mutated = RequestHeaders.modify(exchange,
                h -> h.set(GatewayConstants.HEADER_CORRELATION_ID, correlationId));
        mutated.getAttributes().put(GatewayConstants.ATTR_CORRELATION_ID, correlationId);
        mutated.getAttributes().put(GatewayConstants.ATTR_CLIENT_IP, clientIpResolver.clientIp(exchange));
        mutated.getAttributes().put(GatewayConstants.ATTR_CLIENT_PROTO, clientIpResolver.clientProto(exchange));
        mutated.getResponse().getHeaders().set(GatewayConstants.HEADER_CORRELATION_ID, correlationId);
        return chain.filter(mutated);
    }
}