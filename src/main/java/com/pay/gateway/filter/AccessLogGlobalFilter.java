package com.pay.gateway.filter;

import com.pay.gateway.constant.GatewayConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * One line per proxied request: method, path (no query string), status, duration, client IP, correlation id.
 * Never logs bodies, Authorization, cookies or X-E2E-* values.
 */
@Slf4j
@Component
public class AccessLogGlobalFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long start = System.nanoTime();
        return chain.filter(exchange).doFinally(signal -> {
            HttpStatusCode status = exchange.getResponse().getStatusCode();
            log.info("{} {} -> {} in {}ms ip={} cid={}",
                    exchange.getRequest().getMethod(),
                    exchange.getRequest().getPath().value(),
                    status == null ? "-" : status.value(),
                    (System.nanoTime() - start) / 1_000_000,
                    exchange.getAttribute(GatewayConstants.ATTR_CLIENT_IP),
                    exchange.getAttribute(GatewayConstants.ATTR_CORRELATION_ID));
        });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
