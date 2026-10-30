package com.pay.gateway.config;

import com.pay.gateway.config.properties.GatewayProperties;
import com.pay.gateway.constant.GatewayConstants;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Routes. RULE: paths are forwarded UNCHANGED — no StripPrefix/RewritePath on encrypted routes.
 * E2E encryption seals METHOD|PATH into every payload (AAD); a rewritten path makes every encrypted
 * request fail with AUTH-4003. Bodies are streamed through untouched.
 * /dev/** and /actuator/** of services are deliberately NOT routed.
 */
@Configuration
public class RouteConfig {

    /** Both the gateway and the service add CORS headers; browsers reject duplicates. */
    private static final String DUPLICATE_RESPONSE_HEADERS =
        "Access-Control-Allow-Origin Access-Control-Allow-Credentials Access-Control-Expose-Headers Vary X-Correlation-Id";
    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder, GatewayProperties properties) {
        return builder.routes()
                .route(GatewayConstants.AUTH_SERVICE_ROUTE, r -> r
                        .path("/api/v1/auth/**", "/api/v1/users/**", "/api/v1/admin/**",
                              "/api/v1/crypto/**", "/oauth2/jwks")
                        .filters(f -> f
                                .circuitBreaker(cb -> cb
                                        .setName(GatewayConstants.AUTH_CIRCUIT_BREAKER)
                                        .setFallbackUri("forward:/fallback/" + GatewayConstants.AUTH_SERVICE_ROUTE))
                                .dedupeResponseHeader(DUPLICATE_RESPONSE_HEADERS, "RETAIN_UNIQUE"))
                        .uri(properties.authServiceUri()))
                .build();
    }
}
