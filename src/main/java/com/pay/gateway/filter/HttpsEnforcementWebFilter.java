package com.pay.gateway.filter;

import com.pay.gateway.config.properties.GatewayProperties;
import com.pay.gateway.constant.GatewayConstants;
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

/** Rejects non-HTTPS traffic at the edge (app.gateway.require-https=true in staging/prod). */
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class HttpsEnforcementWebFilter implements WebFilter {

    private final GatewayProperties properties;
    private final JsonErrorWriter errorWriter;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (!properties.requireHttps()
                || "https".equals(exchange.getAttribute(GatewayConstants.ATTR_CLIENT_PROTO))
                || exchange.getRequest().getPath().value().startsWith("/actuator/health")) {
            return chain.filter(exchange);
        }
        return errorWriter.write(exchange, GatewayErrorCode.HTTPS_REQUIRED);
    }
}
