package com.pay.gateway.ratelimit;

import com.pay.gateway.config.properties.GatewayProperties;
import com.pay.gateway.constant.GatewayConstants;
import com.pay.gateway.constant.enums.GatewayErrorCode;
import com.pay.gateway.util.JsonErrorWriter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Coarse per-client-IP limit on everything that enters the platform, applied BEFORE JWT validation
 * so floods are rejected cheaply. Endpoint-specific limits (e.g. 10 logins/min) stay in each service.
 * Returns the standard JSON envelope with Retry-After (not an empty 429).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 30)
public class EdgeRateLimitWebFilter implements WebFilter {

    private final GatewayProperties properties;
    private final EdgeRateLimiter rateLimiter;
    private final JsonErrorWriter errorWriter;
    private final MeterRegistry meterRegistry;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        GatewayProperties.RateLimit config = properties.rateLimit();
        if (!config.enabled() || exchange.getRequest().getPath().value().startsWith("/actuator")) {
            return chain.filter(exchange);
        }
        String clientIp = exchange.getAttribute(GatewayConstants.ATTR_CLIENT_IP);
        return rateLimiter.tryAcquire("ip:" + clientIp, config.limit(), config.window())
                .flatMap(decision -> {
                    if (decision.allowed()) {
                        return chain.filter(exchange);
                    }
                    meterRegistry.counter("gateway.rate_limited").increment();
                    log.warn("Edge rate limit hit ip={} path={}", clientIp, exchange.getRequest().getPath().value());
                    return errorWriter.write(exchange, GatewayErrorCode.TOO_MANY_REQUESTS,
                            Map.of(HttpHeaders.RETRY_AFTER, Long.toString(decision.retryAfterSeconds())));
                });
    }
}
